package com.segment.analytics;

import android.app.Activity;
import android.app.Application;
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
import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Analytics.Callback;
import static com.segment.analytics.internal.Utils.THREAD_PREFIX;
import static com.segment.analytics.internal.Utils.buffer;
import static com.segment.analytics.internal.Utils.closeQuietly;
import static com.segment.analytics.internal.Utils.debug;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * The class that forwards operations from the client to integrations, including Segment. It
 * maintains it's own in-memory queue to queue events while we're fetching the project settings
 * from our server. Once it enables all integrations,it replays any events in the queue. Subsequent
 * launches will be use the cached settings on disk.
 */
class IntegrationManager implements Application.ActivityLifecycleCallbacks {

  private static final String INTEGRATION_MANAGER_THREAD_NAME =
      THREAD_PREFIX + "IntegrationManager";
  private static final long SETTINGS_REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // 24 hours
  private static final long SETTINGS_RETRY_INTERVAL = 1000 * 60; // 1 minute

  final Map<String, Boolean> bundledIntegrations = new ConcurrentHashMap<>();
  final List<AbstractIntegration> integrations = new ArrayList<>();

  final Analytics analytics;
  final Application application;
  final Client client;
  final Cartographer cartographer;
  final Stats stats;
  final ProjectSettings.Cache projectSettingsCache;
  final Analytics.LogLevel logLevel;
  final HandlerThread integrationManagerThread;
  final Handler integrationManagerHandler;
  final ExecutorService networkExecutor;
  final SegmentDispatcher segmentDispatcher; // Keep around for shutdown

  Queue<IntegrationOperation> operationQueue;
  Map<String, Callback> callbacks;
  volatile boolean initialized;

  static synchronized IntegrationManager create(Analytics analytics, Cartographer cartographer,
      Client client, ExecutorService networkExecutor, Stats stats, String tag,
      long flushIntervalInMillis, int flushQueueSize) {
    ProjectSettings.Cache projectSettingsCache =
        new ProjectSettings.Cache(analytics.getApplication(), cartographer, tag);
    return new IntegrationManager(analytics, client, networkExecutor, cartographer, stats,
        projectSettingsCache, tag, flushIntervalInMillis, flushQueueSize);
  }

  IntegrationManager(Analytics analytics, Client client, ExecutorService networkExecutor,
      Cartographer cartographer, Stats stats, ProjectSettings.Cache projectSettingsCache,
      String tag, long flushIntervalInMillis, int flushQueueSize) {
    this.analytics = analytics;
    this.application = analytics.getApplication();
    this.client = client;
    this.networkExecutor = networkExecutor;
    this.cartographer = cartographer;
    this.stats = stats;
    this.projectSettingsCache = projectSettingsCache;
    this.logLevel = analytics.getLogLevel();

    application.registerActivityLifecycleCallbacks(this);

    integrationManagerThread =
        new HandlerThread(INTEGRATION_MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    integrationManagerThread.start();
    integrationManagerHandler =
        new IntegrationManagerHandler(integrationManagerThread.getLooper(), this);

    loadIntegrations();
    segmentDispatcher =
        SegmentDispatcher.create(application, client, cartographer, networkExecutor, stats,
            Collections.unmodifiableMap(bundledIntegrations), tag, flushIntervalInMillis,
            flushQueueSize, logLevel);
    integrations.add(segmentDispatcher);

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

  private void loadIntegrations() {
    loadIntegration("com.segment.analytics.internal.integrations.AmplitudeIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.AppsFlyerIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.ApptimizeIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.BugsnagIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.CountlyIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.CrittercismIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.FlurryIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.GoogleAnalyticsIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.KahunaIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.LeanplumIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.LocalyticsIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.MixpanelIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.QuantcastIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.TapstreamIntegration");
    loadIntegration("com.segment.analytics.internal.integrations.TaplyticsIntegration");
  }

  /**
   * Checks if an integration with the give class name is available on the classpath, i.e. if it
   * was bundled at compile time. If it is, this will attempt to load the integration.
   */
  void loadIntegration(String className) {
    try {
      Class clazz = Class.forName(className);
      loadIntegration(clazz);
    } catch (ClassNotFoundException e) {
      if (logLevel.log()) {
        debug("Integration for class %s not bundled.", className);
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
      throw new AssertionError(
          "Could not create instance of " + clazz.getCanonicalName() + ".\n" + e);
    }
  }

  void dispatchFetchSettings() {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_FETCH_SETTINGS));
  }

  private void dispatchRetryFetchSettings() {
    integrationManagerHandler.sendMessageDelayed(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_FETCH_SETTINGS), SETTINGS_RETRY_INTERVAL);
  }

  void performFetchSettings() {
    if (!isConnected(application)) {
      dispatchRetryFetchSettings();
      return;
    }

    try {
      ProjectSettings projectSettings = networkExecutor.submit(new Callable<ProjectSettings>() {
        @Override public ProjectSettings call() throws Exception {
          Client.Connection connection = null;
          try {
            connection = client.fetchSettings();
            Map<String, Object> map = cartographer.fromJson(buffer(connection.is));
            return ProjectSettings.create(map);
          } finally {
            closeQuietly(connection);
          }
        }
      }).get();
      projectSettingsCache.set(projectSettings);
      dispatchInitializeIntegrations(projectSettings);
    } catch (InterruptedException e) {
      if (logLevel.log()) {
        error(e, "Thread interrupted while fetching settings.");
      }
    } catch (ExecutionException e) {
      if (logLevel.log()) {
        error(e, "Unable to fetch settings. Retrying in %s ms.", SETTINGS_RETRY_INTERVAL);
      }
      dispatchRetryFetchSettings();
    }
  }

  void dispatchInitializeIntegrations(ProjectSettings projectSettings) {
    integrationManagerHandler.sendMessageAtFrontOfQueue(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_INITIALIZE_INTEGRATIONS, projectSettings));
  }

  void performInitializeIntegrations(ProjectSettings projectSettings) {
    if (initialized) return;

    ValueMap integrationSettings = projectSettings.integrations();
    if (isNullOrEmpty(integrationSettings)) {
      if (logLevel.log()) {
        error(null, "No integrations enabled in %s. Make sure you have the correct writeKey.",
            projectSettings);
      }
      bundledIntegrations.clear();
      integrations.clear();
    } else {
      if (logLevel.log()) {
        debug("Initializing integrations with %s.", integrationSettings);
      }
      initializeIntegrations(integrationSettings);
    }

    if (callbacks != null) {
      // clear out callbacks for integrations that may not have been initialized
      callbacks.clear();
      callbacks = null;
    }

    replay();
    initialized = true;
  }

  private void initializeIntegrations(ValueMap integrationSettings) {
    Iterator<AbstractIntegration> iterator = integrations.iterator();
    while (iterator.hasNext()) {
      AbstractIntegration integration = iterator.next();
      String key = integration.key();
      ValueMap settings = integrationSettings.getValueMap(key);
      boolean initializedIntegration = false;
      if (!isNullOrEmpty(settings)) {
        if (logLevel.log()) {
          debug("Initializing integration %s with settings %s.", key, settings);
        }
        try {
          integration.initialize(analytics, settings);
          initializedIntegration = true;
        } catch (Exception e) {
          if (logLevel.log()) {
            error(e, "Could not initialize integration %s.", key);
          }
        }
      }
      if (initializedIntegration) {
        if (!isNullOrEmpty(callbacks)) {
          Callback callback = callbacks.get(key);
          if (callback != null) {
            callback.onReady(integration.getUnderlyingInstance());
          }
        }
      } else {
        iterator.remove();
        bundledIntegrations.remove(key);
      }
    }
  }

  private void replay() {
    if (!isNullOrEmpty(operationQueue)) {
      Iterator<IntegrationOperation> operationIterator = operationQueue.iterator();
      while (operationIterator.hasNext()) {
        IntegrationOperation operation = operationIterator.next();
        run(operation);
        operationIterator.remove();
      }
    }
    operationQueue = null;
  }

  @Override
  public void onActivityCreated(final Activity activity, final Bundle savedInstanceState) {
    dispatchEnqueue(IntegrationOperation.onActivityCreated(activity, savedInstanceState));
  }

  @Override public void onActivityStarted(Activity activity) {
    dispatchEnqueue(IntegrationOperation.onActivityStarted(activity));
  }

  @Override public void onActivityResumed(Activity activity) {
    dispatchEnqueue(IntegrationOperation.onActivityResumed(activity));
  }

  @Override public void onActivityPaused(Activity activity) {
    dispatchEnqueue(IntegrationOperation.onActivityPaused(activity));
  }

  @Override public void onActivityStopped(Activity activity) {
    dispatchEnqueue(IntegrationOperation.onActivityStopped(activity));
  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    dispatchEnqueue(IntegrationOperation.onActivitySaveInstanceState(activity, outState));
  }

  @Override public void onActivityDestroyed(Activity activity) {
    dispatchEnqueue(IntegrationOperation.onActivityDestroyed(activity));
  }

  private void dispatchEnqueue(IntegrationOperation operation) {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_ENQUEUE_OPERATION, operation));
  }

  void dispatchFlush() {
    dispatchEnqueue(IntegrationOperation.flush());
  }

  public void dispatchReset() {
    dispatchEnqueue(IntegrationOperation.reset());
  }

  void dispatchEnqueue(BasePayload basePayload) {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_ENQUEUE_PAYLOAD, basePayload));
  }

  void performEnqueue(BasePayload payload) {
    IntegrationOperation operation;
    switch (payload.type()) {
      case identify:
        operation = IntegrationOperation.identify((IdentifyPayload) payload);
        break;
      case alias:
        operation = IntegrationOperation.alias((AliasPayload) payload);
        break;
      case group:
        operation = IntegrationOperation.group((GroupPayload) payload);
        break;
      case track:
        operation = IntegrationOperation.track((TrackPayload) payload);
        break;
      case screen:
        operation = IntegrationOperation.screen((ScreenPayload) payload);
        break;
      default:
        throw new AssertionError("unknown type " + payload.type());
    }
    performEnqueue(operation);
  }

  void performEnqueue(IntegrationOperation operation) {
    if (initialized) {
      run(operation);
    } else {
      if (logLevel.log()) {
        debug("Enqueuing action %s.", operation);
      }
      if (operationQueue == null) {
        operationQueue = new ArrayDeque<>();
      }
      operationQueue.add(operation);
    }
  }

  /** Runs the given operation on all bundled integrations. */
  void run(IntegrationOperation operation) {
    if (logLevel.log()) {
      debug("Running %s on %s integrations.", operation, integrations.size());
    }
    for (int i = 0; i < integrations.size(); i++) {
      AbstractIntegration integration = integrations.get(i);
      long startTime = System.nanoTime();
      operation.run(integration, projectSettingsCache.get());
      long endTime = System.nanoTime();
      long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
      if (logLevel.log()) {
        debug("Took %s ms to run action %s on %s.", duration, operation, integration.key());
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
    application.unregisterActivityLifecycleCallbacks(this);
    integrationManagerThread.quit();
    segmentDispatcher.shutdown();
    if (operationQueue != null) {
      operationQueue.clear();
      operationQueue = null;
    }
  }

  interface Factory {
    // todo: remove circular dependency!!!
    IntegrationManager create(Analytics analytics);
  }

  static class IntegrationManagerHandler extends Handler {

    static final int REQUEST_FETCH_SETTINGS = 1;
    static final int REQUEST_INITIALIZE_INTEGRATIONS = 2;
    private static final int REQUEST_ENQUEUE_OPERATION = 3;
    private static final int REQUEST_ENQUEUE_PAYLOAD = 4;
    private static final int REQUEST_REGISTER_CALLBACK = 5;
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
        case REQUEST_ENQUEUE_OPERATION:
          integrationManager.performEnqueue((IntegrationOperation) msg.obj);
          break;
        case REQUEST_ENQUEUE_PAYLOAD:
          integrationManager.performEnqueue((BasePayload) msg.obj);
          break;
        case REQUEST_REGISTER_CALLBACK:
          //noinspection unchecked
          Pair<String, Analytics.Callback> pair = (Pair<String, Analytics.Callback>) msg.obj;
          integrationManager.performRegisterCallback(pair.first, pair.second);
          break;
        default:
          throw new AssertionError("Unknown Integration Manager handler message: " + msg);
      }
    }
  }
}
