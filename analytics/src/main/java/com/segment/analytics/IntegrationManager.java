package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Analytics.OnIntegrationReadyListener;
import static com.segment.analytics.Logger.OWNER_INTEGRATION_MANAGER;
import static com.segment.analytics.Logger.VERB_DISPATCH;
import static com.segment.analytics.Logger.VERB_ENQUEUE;
import static com.segment.analytics.Logger.VERB_INITIALIZE;
import static com.segment.analytics.Utils.THREAD_PREFIX;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.isNullOrEmpty;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;

/**
 * The class that forwards operations from the client to integrations, including Segment.
 * It maintains it's own in-memory queue to account for the latency between receiving the first
 * event, fetching settings from the server and enabling the integrations. Once it enables all
 * integrations it replays any events in the queue. This will only affect the first app install,
 * subsequent launches will be use the cached settings on disk.
 */
class IntegrationManager {
  private static final String PROJECT_SETTINGS_CACHE_KEY_PREFIX = "project-settings-";
  private static final String MANAGER_THREAD_NAME = THREAD_PREFIX + "IntegrationManager";
  private static final long SETTINGS_REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // 24 hours
  private static final long SETTINGS_ERROR_RETRY_INTERVAL = 1000 * 60; // 1 minute

  final Context context;
  final SegmentHTTPApi segmentHTTPApi;
  final HandlerThread networkingThread;
  final Handler networkingHandler;
  final Handler integrationManagerHandler;
  final Stats stats;
  final boolean debuggingEnabled;
  final ValueMap.Cache<ProjectSettings> projectSettingsCache;
  final Logger logger;
  final List<AbstractIntegration> integrations = new CopyOnWriteArrayList<AbstractIntegration>();
  final Map<String, Boolean> bundledIntegrations = new ConcurrentHashMap<String, Boolean>();
  Queue<IntegrationOperation> operationQueue;
  boolean initialized;
  OnIntegrationReadyListener listener;

  static synchronized IntegrationManager create(Context context, SegmentHTTPApi segmentHTTPApi,
      Stats stats, Logger logger, String tag, boolean debuggingEnabled) {
    ValueMap.Cache<ProjectSettings> projectSettingsCache =
        new ValueMap.Cache<ProjectSettings>(context, PROJECT_SETTINGS_CACHE_KEY_PREFIX + tag,
            ProjectSettings.class);
    return new IntegrationManager(context, segmentHTTPApi, projectSettingsCache, stats, logger,
        debuggingEnabled);
  }

  IntegrationManager(Context context, SegmentHTTPApi segmentHTTPApi,
      ValueMap.Cache<ProjectSettings> projectSettingsCache, Stats stats, Logger logger,
      boolean debuggingEnabled) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.stats = stats;
    this.debuggingEnabled = debuggingEnabled;
    this.logger = logger;
    networkingThread = new HandlerThread(MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    networkingThread.start();
    networkingHandler = new NetworkingHandler(networkingThread.getLooper(), this);
    integrationManagerHandler = new IntegrationHandler(Looper.getMainLooper(), this);

    // Look up all the integrations available on the device. This is done early so that we can
    // disable sending to these integrations from the server and properly fill the payloads with
    // this information
    loadBundledIntegration("com.segment.analytics.AmplitudeIntegration");
    loadBundledIntegration("com.segment.analytics.AppsFlyerIntegration");
    loadBundledIntegration("com.segment.analytics.BugsnagIntegration");
    loadBundledIntegration("com.segment.analytics.CountlyIntegration");
    loadBundledIntegration("com.segment.analytics.CrittercismIntegration");
    loadBundledIntegration("com.segment.analytics.FlurryIntegration");
    loadBundledIntegration("com.segment.analytics.GoogleAnalyticsIntegration");
    loadBundledIntegration("com.segment.analytics.KahunaIntegration");
    loadBundledIntegration("com.segment.analytics.LeanplumIntegration");
    loadBundledIntegration("com.segment.analytics.LocalyticsIntegration");
    loadBundledIntegration("com.segment.analytics.MixpanelIntegration");
    loadBundledIntegration("com.segment.analytics.QuantcastIntegration");
    loadBundledIntegration("com.segment.analytics.TapstreamIntegration");

    this.projectSettingsCache = projectSettingsCache;

    if (!projectSettingsCache.isSet() || projectSettingsCache.get() == null) {
      dispatchFetch();
    } else {
      ProjectSettings projectSettings = projectSettingsCache.get();
      dispatchInitialize(projectSettings);
      if (projectSettings.timestamp() + SETTINGS_REFRESH_INTERVAL < System.currentTimeMillis()) {
        // Update stale settings
        dispatchFetch();
      }
    }
  }

  private void loadBundledIntegration(String className) {
    try {
      Class clz = Class.forName(className);
      AbstractIntegration integration = (AbstractIntegration) clz.newInstance();
      integrations.add(integration);
      bundledIntegrations.put(integration.key(), false);
    } catch (InstantiationException e) {
      logger.print(e, "Skipped integration %s as it could not be instantiated.", className);
    } catch (ClassNotFoundException e) {
      logger.print(e, "Skipped integration %s as it was not bundled.", className);
    } catch (IllegalAccessException e) {
      logger.print(e, "Skipped integration %s as it could not be accessed.", className);
    }
  }

  void dispatchFetch() {
    networkingHandler.sendMessage(
        networkingHandler.obtainMessage(NetworkingHandler.REQUEST_FETCH_SETTINGS));
  }

  void performFetch() {
    try {
      if (isConnected(context)) {
        ProjectSettings projectSettings = segmentHTTPApi.fetchSettings();
        projectSettingsCache.set(projectSettings);
        if (!initialized) {
          // It's ok if integrations are being initialized right now (and so initialized will be
          // false). Since we just dispatch the request, the actual perform method will be able to
          // see the real value of `initialized` and just skip the operation
          dispatchInitialize(projectSettings);
        }
      } else {
        retryFetch();
      }
    } catch (IOException e) {
      retryFetch();
    }
  }

  void retryFetch() {
    networkingHandler.sendMessageDelayed(
        networkingHandler.obtainMessage(NetworkingHandler.REQUEST_FETCH_SETTINGS),
        SETTINGS_ERROR_RETRY_INTERVAL);
  }

  void dispatchInitialize(ProjectSettings projectSettings) {
    integrationManagerHandler.sendMessageAtFrontOfQueue(
        integrationManagerHandler.obtainMessage(IntegrationHandler.REQUEST_INITIALIZE,
            projectSettings));
  }

  void performInitialize(ProjectSettings projectSettings) {
    if (initialized) return;

    // Iterate over all the bundled integrations
    Iterator<AbstractIntegration> iterator = integrations.iterator();
    while (iterator.hasNext()) {
      AbstractIntegration integration = iterator.next();
      String key = integration.key();
      if (projectSettings.containsKey(key)) {
        ValueMap settings = new ValueMap(projectSettings.getValueMap(key));
        try {
          logger.debug(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, key, "settings: %s", settings);
          integration.initialize(context, settings, debuggingEnabled);
          if (listener != null) {
            listener.onIntegrationReady(key, integration.getUnderlyingInstance());
            listener = null; // clear the reference
          }
        } catch (IllegalStateException e) {
          iterator.remove();
          bundledIntegrations.remove(key);
          logger.print(e, "Did not initialize integration %s as it needed more permissions.", key);
        }
      } else {
        iterator.remove();
        logger.print(null, "Did not initialize integration %s as it was disabled.", key);
      }
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
    dispatchOperation(new FlushOperation());
  }

  void dispatchOperation(IntegrationOperation operation) {
    integrationManagerHandler.sendMessage(
        integrationManagerHandler.obtainMessage(IntegrationHandler.REQUEST_DISPATCH_OPERATION,
            operation));
  }

  void performOperation(IntegrationOperation operation) {
    if (initialized) {
      run(operation);
    } else {
      if (operationQueue == null) {
        operationQueue = new ArrayDeque<IntegrationOperation>();
      }
      logger.debug(OWNER_INTEGRATION_MANAGER, VERB_ENQUEUE, operation.id(), null);
      operationQueue.add(operation);
    }
  }

  /** Runs the given operation on all Bundled integrations. */
  private void run(IntegrationOperation operation) {
    for (int i = 0; i < integrations.size(); i++) {
      AbstractIntegration integration = integrations.get(i);
      long startTime = System.currentTimeMillis();
      operation.run(integration);
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;
      logger.debug(integration.key(), VERB_DISPATCH, operation.id(), "duration: %s", duration);
      stats.dispatchIntegrationOperation(duration);
    }
  }

  void dispatchRegisterIntegrationInitializedListener(OnIntegrationReadyListener listener) {
    integrationManagerHandler.sendMessage(
        integrationManagerHandler.obtainMessage(IntegrationHandler.REQUEST_REGISTER_LISTENER,
            listener));
  }

  void performRegisterIntegrationInitializedListener(OnIntegrationReadyListener listener) {
    if (initialized && listener != null) {
      // Integrations are already ready, notify the listener right away
      for (AbstractIntegration abstractIntegration : integrations) {
        listener.onIntegrationReady(abstractIntegration.key(),
            abstractIntegration.getUnderlyingInstance());
      }
    } else {
      this.listener = listener;
    }
  }

  void shutdown() {
    quitThread(networkingThread);
    if (operationQueue != null) {
      operationQueue.clear();
      operationQueue = null;
    }
  }

  interface IntegrationOperation {
    void run(AbstractIntegration integration);

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

    @Override public void run(AbstractIntegration integration) {
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
          panic("Unknown lifecycle event type!" + type);
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

  static class FlushOperation implements IntegrationOperation {
    final String id;

    FlushOperation() {
      this.id = UUID.randomUUID().toString();
    }

    @Override public void run(AbstractIntegration integration) {
      integration.flush();
    }

    @Override public String id() {
      return id;
    }

    @Override public String toString() {
      return "Flush";
    }
  }

  private static class NetworkingHandler extends Handler {
    static final int REQUEST_FETCH_SETTINGS = 1;
    private final IntegrationManager integrationManager;

    NetworkingHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_FETCH_SETTINGS:
          integrationManager.performFetch();
          break;
        default:
          panic("Unhandled dispatcher message." + msg.what);
      }
    }
  }

  private static class IntegrationHandler extends Handler {
    static final int REQUEST_INITIALIZE = 1;
    static final int REQUEST_DISPATCH_OPERATION = 2;
    static final int REQUEST_REGISTER_LISTENER = 3;

    private final IntegrationManager integrationManager;

    IntegrationHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_INITIALIZE:
          integrationManager.performInitialize((ProjectSettings) msg.obj);
          break;
        case REQUEST_DISPATCH_OPERATION:
          integrationManager.performOperation((IntegrationOperation) msg.obj);
          break;
        case REQUEST_REGISTER_LISTENER:
          integrationManager.performRegisterIntegrationInitializedListener(
              (OnIntegrationReadyListener) msg.obj);
          break;
        default:
          panic("Unhandled dispatcher message." + msg.what);
      }
    }
  }
}
