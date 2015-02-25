package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.BasePayload;
import com.segment.analytics.internal.model.payloads.GroupPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Analytics.Callback;
import static com.segment.analytics.Options.ALL_INTEGRATIONS_KEY;
import static com.segment.analytics.internal.Utils.OWNER_INTEGRATION_MANAGER;
import static com.segment.analytics.internal.Utils.THREAD_PREFIX;
import static com.segment.analytics.internal.Utils.VERB_DISPATCH;
import static com.segment.analytics.internal.Utils.VERB_DOWNLOAD;
import static com.segment.analytics.internal.Utils.VERB_ENQUEUE;
import static com.segment.analytics.internal.Utils.VERB_INITIALIZE;
import static com.segment.analytics.internal.Utils.VERB_SKIP;
import static com.segment.analytics.internal.Utils.buffer;
import static com.segment.analytics.internal.Utils.closeQuietly;
import static com.segment.analytics.internal.Utils.debug;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.panic;

/**
 * The class that forwards operations from the client to integrations, including Segment. It
 * maintains it's own in-memory queue to account for the latency between receiving the first event,
 * fetching settings from the server and enabling the integrations. Once it enables all
 * integrations,it replays any events in the queue. Subsequent launches will be use the cached
 * settings on disk.
 */
class IntegrationManager {
  private static final String INTEGRATION_MANAGER_THREAD_NAME =
      THREAD_PREFIX + OWNER_INTEGRATION_MANAGER;
  private static final long SETTINGS_REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // 24 hours
  private static final long SETTINGS_RETRY_INTERVAL = 1000 * 60; // 1 minute

  final Map<String, Boolean> bundledIntegrations = new ConcurrentHashMap<>();
  final List<AbstractIntegration> integrations = new ArrayList<>();

  final Context context;
  final Client client;
  final Cartographer cartographer;
  final Stats stats;
  final ProjectSettings.Cache projectSettingsCache;
  final Analytics.LogLevel logLevel;
  final HandlerThread integrationManagerThread;
  final Handler integrationManagerHandler;
  final ExecutorService networkExecutor;

  Queue<IntegrationOperation> operationQueue;
  Map<String, Callback> callbacks;
  volatile boolean initialized;

  static synchronized IntegrationManager create(Context context, Cartographer cartographer,
      Client client, ExecutorService networkExecutor, Stats stats, String tag,
      Analytics.LogLevel logLevel) {
    ProjectSettings.Cache projectSettingsCache =
        new ProjectSettings.Cache(context, cartographer, tag);
    return new IntegrationManager(context, client, networkExecutor, cartographer, stats,
        projectSettingsCache, logLevel);
  }

  IntegrationManager(Context context, Client client, ExecutorService networkExecutor,
      Cartographer cartographer, Stats stats, ProjectSettings.Cache projectSettingsCache,
      Analytics.LogLevel logLevel) {
    this.context = context;
    this.client = client;
    this.networkExecutor = networkExecutor;
    this.cartographer = cartographer;
    this.stats = stats;
    this.projectSettingsCache = projectSettingsCache;
    this.logLevel = logLevel;

    integrationManagerThread =
        new HandlerThread(INTEGRATION_MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    integrationManagerThread.start();
    integrationManagerHandler =
        new IntegrationManagerHandler(integrationManagerThread.getLooper(), this);

    checkBundledIntegration("com.segment.analytics.internal.integrations.AmplitudeIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.AppsFlyerIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.BugsnagIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.CountlyIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.CrittercismIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.FlurryIntegration");
    checkBundledIntegration(
        "com.segment.analytics.internal.integrations.GoogleAnalyticsIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.KahunaIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.LeanplumIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.LocalyticsIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.MixpanelIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.QuantcastIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.TapstreamIntegration");

    if (projectSettingsCache.isSet() && projectSettingsCache.get() != null) {
      dispatchInitializeIntegrations(projectSettingsCache.get());
      if (projectSettingsCache.get().timestamp() + SETTINGS_REFRESH_INTERVAL
          < System.currentTimeMillis()) {
        dispatchFetchSettings();
      }
    } else {
      dispatchFetchSettings();
    }
  }

  /**
   * Checks if an integration with the give class name is available on the classpath, i.e. if it
   * was bundled at compile time. If it is, this will attempt to load the integration.
   */
  void checkBundledIntegration(String className) {
    try {
      Class clazz = Class.forName(className);
      loadIntegration(clazz);
    } catch (ClassNotFoundException e) {
      if (logLevel.log()) {
        debug(OWNER_INTEGRATION_MANAGER, VERB_SKIP, className);
      }
    }
  }

  /**
   * Instantiates an instance of {@link AbstractIntegration} for the given class. The {@link
   * AbstractIntegration} *MUST* have an empty constructor. This will also update the {@code
   * bundledIntegrations} map so that events for this integration aren't sent server side.
   * <p/>
   * This will not initialize the integration.
   */
  private void loadIntegration(Class<AbstractIntegration> clazz) {
    try {
      Constructor<AbstractIntegration> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      AbstractIntegration integration = constructor.newInstance();
      integrations.add(integration);
      bundledIntegrations.put(integration.key(), false);
    } catch (Exception e) {
      throw panic(e, "Could not instantiate class.");
    }
  }

  void dispatchFetchSettings() {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_FETCH_SETTINGS));
  }

  void dispatchRetryFetchSettings() {
    integrationManagerHandler.sendMessageDelayed(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_FETCH_SETTINGS), SETTINGS_RETRY_INTERVAL);
  }

  void performFetchSettings() {
    if (!isConnected(context)) {
      dispatchRetryFetchSettings();
      return;
    }

    try {
      ProjectSettings projectSettings = networkExecutor.submit(new Callable<ProjectSettings>() {
        @Override public ProjectSettings call() throws Exception {
          return fetchSettings();
        }
      }).get();
      projectSettingsCache.set(projectSettings);
      dispatchInitializeIntegrations(projectSettings);
    } catch (InterruptedException e) {
      if (logLevel.log()) {
        error(OWNER_INTEGRATION_MANAGER, VERB_DOWNLOAD, null, e,
            "Interrupted while fetching settings.");
      }
    } catch (ExecutionException e) {
      if (logLevel.log()) {
        error(OWNER_INTEGRATION_MANAGER, VERB_DOWNLOAD, null, e, "Unable to fetch settings");
      }
      dispatchRetryFetchSettings();
    }
  }

  private ProjectSettings fetchSettings() throws IOException {
    Client.Connection connection = null;
    try {
      connection = client.fetchSettings();
      Map<String, Object> map = cartographer.fromJson(buffer(connection.is));
      return ProjectSettings.create(map);
    } catch (IOException e) {
      if (logLevel.log()) {
        error(OWNER_INTEGRATION_MANAGER, VERB_DOWNLOAD, null, e, "Unable to fetch settings");
      }
      throw e;
    } finally {
      closeQuietly(connection);
    }
  }

  void dispatchInitializeIntegrations(ProjectSettings projectSettings) {
    integrationManagerHandler.sendMessageAtFrontOfQueue(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_INITIALIZE_INTEGRATIONS, projectSettings));
  }

  void performInitializeIntegrations(ProjectSettings projectSettings) {
    if (initialized) return;

    ValueMap integrationSettings = projectSettings.integrations();
    Iterator<AbstractIntegration> iterator = integrations.iterator();
    while (iterator.hasNext()) {
      AbstractIntegration integration = iterator.next();
      String key = integration.key();
      if (!isNullOrEmpty(integrationSettings) || integrationSettings.containsKey(key)) {
        ValueMap settings = integrationSettings.getValueMap(key);
        try {
          if (logLevel.log()) {
            debug(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, key, settings);
          }
          integration.initialize(context, settings, logLevel);
          if (!isNullOrEmpty(callbacks)) {
            Callback callback = callbacks.get(key);
            if (callback != null) {
              callback.onReady(integration.getUnderlyingInstance());
            }
          }
        } catch (IllegalStateException e) {
          if (logLevel.log()) {
            error(OWNER_INTEGRATION_MANAGER, VERB_SKIP, key, e, settings);
          }
          iterator.remove();
          bundledIntegrations.remove(key);
        }
      } else {
        iterator.remove();
      }
    }

    if (callbacks != null) {
      callbacks.clear();
      callbacks = null;
    }

    if (!isNullOrEmpty(operationQueue)) {
      Iterator<IntegrationOperation> operationIterator = operationQueue.iterator();
      while (operationIterator.hasNext()) {
        IntegrationOperation operation = operationIterator.next();
        run(operation);
        operationIterator.remove();
      }
    }
    operationQueue = null;
    initialized = true;
  }

  void dispatchFlush() {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_DISPATCH_OPERATION, new FlushOperation()));
  }

  void dispatchPayload(BasePayload payload) {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_DISPATCH_OPERATION,
            new PayloadOperation(payload)));
  }

  void dispatchLifecyclePayload(ActivityLifecyclePayload payload) {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_DISPATCH_OPERATION, payload));
  }

  void performOperation(IntegrationOperation operation) {
    if (initialized) {
      run(operation);
    } else {
      if (operationQueue == null) {
        operationQueue = new ArrayDeque<>();
      }
      if (logLevel.log()) {
        debug(OWNER_INTEGRATION_MANAGER, VERB_ENQUEUE, operation.id());
      }
      operationQueue.add(operation);
    }
  }

  /** Runs the given operation on all Bundled integrations. */
  void run(IntegrationOperation operation) {
    for (int i = 0; i < integrations.size(); i++) {
      AbstractIntegration integration = integrations.get(i);
      long startTime = System.nanoTime();
      operation.run(integration, projectSettingsCache.get());
      long endTime = System.nanoTime();
      long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
      if (logLevel.log()) {
        debug(OWNER_INTEGRATION_MANAGER, VERB_DISPATCH, operation.id(), integration.key(),
            duration + "ms");
      }
      stats.dispatchIntegrationOperation(integration.key(), duration);
    }
  }

  void dispatchRegisterCallback(String key, Callback callback) {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_REGISTER_CALLBACK,
            new Pair<>(key, callback)));
  }

  void performRegisterCallback(String key, Callback callback) {
    if (initialized && callback != null) {
      // Integrations are initialized, notify the listener right away
      for (AbstractIntegration abstractIntegration : integrations) {
        if (key.equals(abstractIntegration.key())) {
          callback.onReady(abstractIntegration.getUnderlyingInstance());
        }
      }
    } else {
      if (callback == null) {
        if (callbacks != null) {
          callbacks.remove(key);
        }
      } else {
        if (callbacks == null) {
          callbacks = new HashMap<>();
        }
        callbacks.put(key, callback);
      }
    }
  }

  void shutdown() {
    integrationManagerThread.quit();
    if (operationQueue != null) {
      operationQueue.clear();
      operationQueue = null;
    }
  }

  /** Abstraction for a task that a {@link AbstractIntegration} can execute. */
  interface IntegrationOperation {
    /** Run this operation on the given integration. */
    void run(AbstractIntegration integration, ProjectSettings projectSettings);

    /** Return a unique ID to identify this operation. */
    String id();
  }

  static class ActivityLifecyclePayload implements IntegrationOperation {
    final Type type;
    final Bundle bundle;
    final Activity activity;
    final String id;

    ActivityLifecyclePayload(Type type, Activity activity, Bundle bundle) {
      this.type = type;
      this.bundle = bundle;
      this.id = UUID.randomUUID().toString();
      // Ideally we would store a weak reference, but it doesn't work for stop/destroy events
      this.activity = activity;
    }

    Activity getActivity() {
      return activity;
    }

    @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
      switch (type) {
        case CREATED:
          integration.onActivityCreated(activity, bundle);
          break;
        case STARTED:
          integration.onActivityStarted(activity);
          break;
        case RESUMED:
          integration.onActivityResumed(activity);
          break;
        case PAUSED:
          integration.onActivityPaused(activity);
          break;
        case STOPPED:
          integration.onActivityStopped(activity);
          break;
        case SAVE_INSTANCE:
          integration.onActivitySaveInstanceState(activity, bundle);
          break;
        case DESTROYED:
          integration.onActivityDestroyed(activity);
          break;
        default:
          panic("Unknown lifecycle event type: " + type);
      }
    }

    @Override public String id() {
      return id;
    }

    @Override public String toString() {
      return "ActivityLifecycle{" + type + '}';
    }

    enum Type {
      CREATED, STARTED, RESUMED, PAUSED, STOPPED, SAVE_INSTANCE, DESTROYED
    }
  }

  static class PayloadOperation implements IntegrationOperation {
    final BasePayload payload;

    static boolean isIntegrationEnabled(ValueMap integrations, AbstractIntegration integration) {
      if (isNullOrEmpty(integrations)) {
        return true;
      }
      boolean enabled = true;
      String key = integration.key();
      if (integrations.containsKey(key)) {
        enabled = integrations.getBoolean(key, true);
      } else if (integrations.containsKey(ALL_INTEGRATIONS_KEY)) {
        enabled = integrations.getBoolean(ALL_INTEGRATIONS_KEY, true);
      }
      return enabled;
    }

    static boolean isIntegrationEnabledInPlan(ValueMap plan, AbstractIntegration integration) {
      boolean eventEnabled = plan.getBoolean("enabled", true);
      if (eventEnabled) {
        // The event is enabled in the tracking plan. Check if there is an integration
        // specific setting.
        ValueMap integrationPlan = plan.getValueMap("integrations");
        if (!isIntegrationEnabled(integrationPlan, integration)) {
          eventEnabled = false;
        }
      }
      return eventEnabled;
    }

    PayloadOperation(BasePayload payload) {
      this.payload = payload;
    }

    @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
      if (!isIntegrationEnabled(payload.integrations(), integration)) {
        return;
      }

      BasePayload.Type type = payload.type();
      switch (type) {
        case track:
          TrackPayload trackPayload = (TrackPayload) payload;
          ValueMap trackingPlan = projectSettings.trackingPlan();
          boolean trackEnabled = true;

          // If tracking plan is empty, leave the event enabled.
          if (!isNullOrEmpty(trackingPlan)) {
            String event = trackPayload.event();
            // If tracking plan has no settings for the event, leave the event enabled.
            if (trackingPlan.containsKey(event)) {
              ValueMap eventPlan = trackingPlan.getValueMap(event);
              trackEnabled = isIntegrationEnabledInPlan(eventPlan, integration);
            }
          }

          if (trackEnabled) {
            integration.track(trackPayload);
          }
          break;
        case identify:
          integration.identify((IdentifyPayload) payload);
          break;
        case alias:
          integration.alias((AliasPayload) payload);
          break;
        case group:
          integration.group((GroupPayload) payload);
          break;
        case screen:
          integration.screen((ScreenPayload) payload);
          break;
        default:
          panic("Unknown payload type: " + type);
      }
    }

    @Override public String id() {
      return payload.messageId();
    }
  }

  static class FlushOperation implements IntegrationOperation {
    String id = UUID.randomUUID().toString();

    @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
      integration.flush();
    }

    @Override public synchronized String id() {
      return id;
    }

    @Override public String toString() {
      return getClass().getCanonicalName();
    }
  }

  static class IntegrationManagerHandler extends Handler {
    static final int REQUEST_FETCH_SETTINGS = 1;
    static final int REQUEST_INITIALIZE_INTEGRATIONS = 2;
    private static final int REQUEST_DISPATCH_OPERATION = 3;
    private static final int REQUEST_REGISTER_CALLBACK = 4;
    private final IntegrationManager integrationManager;

    IntegrationManagerHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_FETCH_SETTINGS:
          integrationManager.performFetchSettings();
          break;
        case REQUEST_INITIALIZE_INTEGRATIONS:
          integrationManager.performInitializeIntegrations((ProjectSettings) msg.obj);
          break;
        case REQUEST_DISPATCH_OPERATION:
          integrationManager.performOperation((IntegrationOperation) msg.obj);
          break;
        case REQUEST_REGISTER_CALLBACK:
          //noinspection unchecked
          Pair<String, Analytics.Callback> pair = (Pair<String, Analytics.Callback>) msg.obj;
          integrationManager.performRegisterCallback(pair.first, pair.second);
          break;
        default:
          panic("Unhandled dispatcher message: " + msg.what);
      }
    }
  }
}
