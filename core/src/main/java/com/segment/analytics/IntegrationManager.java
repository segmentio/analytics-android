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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Analytics.OnIntegrationReadyListener;
import static com.segment.analytics.Logger.OWNER_INTEGRATION_MANAGER;
import static com.segment.analytics.Logger.VERB_DISPATCH;
import static com.segment.analytics.Logger.VERB_INITIALIZE;
import static com.segment.analytics.Logger.VERB_SKIP;
import static com.segment.analytics.Utils.THREAD_PREFIX;
import static com.segment.analytics.Utils.createMap;
import static com.segment.analytics.Utils.getSharedPreferences;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.isNullOrEmpty;
import static com.segment.analytics.Utils.isOnClassPath;
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

  private static final String PROJECT_SETTINGS_CACHE_KEY = "project-settings";
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
  final StringCache projectSettingsCache;
  final SegmentIntegration segmentIntegration;
  final Map<String, Boolean> bundledIntegrations = createMap();
  final Logger logger;
  List<AbstractIntegration> integrations;
  Queue<IntegrationOperation> operationQueue;
  boolean initialized;
  OnIntegrationReadyListener listener;

  IntegrationManager(Context context, SegmentHTTPApi segmentHTTPApi,
      StringCache projectSettingsCache, Stats stats, int queueSize, int flushInterval, String tag,
      Logger logger, boolean debuggingEnabled) {
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
    findBundledIntegration("com.amplitude.api.Amplitude", AmplitudeIntegration.AMPLITUDE_KEY);
    findBundledIntegration("com.appsflyer.AppsFlyerLib", AppsFlyerIntegration.APPS_FLYER_KEY);
    findBundledIntegration("com.bugsnag.android.Bugsnag", BugsnagIntegration.BUGSNAG_KEY);
    findBundledIntegration("ly.count.android.api.Countly", CountlyIntegration.COUNTLY_KEY);
    findBundledIntegration("com.crittercism.app.Crittercism",
        CrittercismIntegration.CRITTERCISM_KEY);
    findBundledIntegration("com.flurry.android.FlurryAgent", FlurryIntegration.FLURRY_KEY);
    findBundledIntegration("com.google.android.gms.analytics.GoogleAnalytics",
        GoogleAnalyticsIntegration.GOOGLE_ANALYTICS_KEY);
    findBundledIntegration("com.kahuna.sdk.KahunaAnalytics", KahunaIntegration.KAHUNA_KEY);
    findBundledIntegration("com.localytics.android.LocalyticsAmpSession",
        LocalyticsIntegration.LOCALYTICS_KEY);
    findBundledIntegration("com.leanplum.Leanplum", LeanplumIntegration.LEANPLUM_KEY);
    findBundledIntegration("com.mixpanel.android.mpmetrics.MixpanelAPI",
        MixpanelIntegration.MIXPANEL_KEY);
    findBundledIntegration("com.quantcast.measurement.service.QuantcastClient",
        QuantcastIntegration.QUANTCAST_KEY);
    findBundledIntegration("com.tapstream.sdk.Tapstream", TapstreamIntegration.TAPSTREAM_KEY);

    segmentIntegration =
        SegmentIntegration.create(context, queueSize, flushInterval, segmentHTTPApi,
            bundledIntegrations, tag, stats, logger);

    this.projectSettingsCache = projectSettingsCache;

    ProjectSettings projectSettings = ProjectSettings.load(projectSettingsCache);
    if (projectSettings == null) {
      dispatchFetch();
    } else {
      performInitialize(projectSettings);
      if (projectSettings.timestamp() + SETTINGS_REFRESH_INTERVAL < System.currentTimeMillis()) {
        dispatchFetch();
      }
    }
  }

  static synchronized IntegrationManager create(Context context, SegmentHTTPApi segmentHTTPApi,
      Stats stats, int queueSize, int flushInterval, String tag, Logger logger,
      boolean debuggingEnabled) {
    StringCache projectSettingsCache =
        new StringCache(getSharedPreferences(context), PROJECT_SETTINGS_CACHE_KEY);
    return new IntegrationManager(context, segmentHTTPApi, projectSettingsCache, stats, queueSize,
        flushInterval, tag, logger, debuggingEnabled);
  }

  private void findBundledIntegration(String className, String key) {
    if (isOnClassPath(className)) bundledIntegrations.put(key, false);
  }

  void dispatchFetch() {
    networkingHandler.sendMessage(
        networkingHandler.obtainMessage(NetworkingHandler.FETCH_SETTINGS));
  }

  void performFetch() {
    try {
      if (isConnected(context)) {
        logger.debug(OWNER_INTEGRATION_MANAGER, "fetch", "settings", null);
        ProjectSettings projectSettings = segmentHTTPApi.fetchSettings();
        String projectSettingsJson = projectSettings.toString();
        projectSettingsCache.set(projectSettingsJson);
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
      logger.debug(OWNER_INTEGRATION_MANAGER, "fetch", "settings", null);
      retryFetch();
    }
  }

  void retryFetch() {
    networkingHandler.sendMessageDelayed(
        networkingHandler.obtainMessage(NetworkingHandler.FETCH_SETTINGS),
        SETTINGS_ERROR_RETRY_INTERVAL);
  }

  void dispatchInitialize(ProjectSettings projectSettings) {
    integrationManagerHandler.sendMessage(
        integrationManagerHandler.obtainMessage(IntegrationHandler.INITIALIZE, projectSettings));
  }

  void performInitialize(ProjectSettings projectSettings) {
    if (initialized) return;

    integrations = new LinkedList<AbstractIntegration>();
    // Iterate over all the bundled integrations
    Iterator<Map.Entry<String, Boolean>> iterator = bundledIntegrations.entrySet().iterator();
    while (iterator.hasNext()) {
      String key = iterator.next().getKey();
      if (projectSettings.containsKey(key)) {
        JsonMap settings = new JsonMap(projectSettings.getJsonMap(key));
        AbstractIntegration integration = createIntegrationForKey(key);
        try {
          logger.debug(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, key, "settings: %s", settings);
          integration.initialize(context, settings, debuggingEnabled);
          if (listener != null) {
            listener.onIntegrationReady(key, integration.getUnderlyingInstance());
          }
          integrations.add(integration);
        } catch (IllegalStateException e) {
          iterator.remove();
          logger.error(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, integration.key(), e, null);
        }
      } else {
        iterator.remove();
        logger.debug(OWNER_INTEGRATION_MANAGER, VERB_SKIP, key, "settings: %s",
            projectSettings.keySet());
      }
    }
    if (!isNullOrEmpty(operationQueue)) {
      Iterator<IntegrationOperation> operationIterator = operationQueue.iterator();
      while (operationIterator.hasNext()) {
        run(operationIterator.next());
        operationIterator.remove();
      }
    }
    operationQueue = null;
    initialized = true;
  }

  AbstractIntegration createIntegrationForKey(String key) {
    switch (key.charAt(0)) {
      case 'A':
        switch (key.charAt(1)) {
          case 'm':
            verify(key, AmplitudeIntegration.AMPLITUDE_KEY);
            return new AmplitudeIntegration();
          case 'p':
            verify(key, AppsFlyerIntegration.APPS_FLYER_KEY);
            return new AppsFlyerIntegration();
          default:
            break;
        }
      case 'B':
        verify(key, BugsnagIntegration.BUGSNAG_KEY);
        return new BugsnagIntegration();
      case 'C':
        switch (key.charAt(1)) {
          case 'o':
            verify(key, CountlyIntegration.COUNTLY_KEY);
            return new CountlyIntegration();
          case 'r':
            verify(key, CrittercismIntegration.CRITTERCISM_KEY);
            return new CrittercismIntegration();
          default:
            break;
        }
      case 'F':
        verify(key, FlurryIntegration.FLURRY_KEY);
        return new FlurryIntegration();
      case 'G':
        verify(key, GoogleAnalyticsIntegration.GOOGLE_ANALYTICS_KEY);
        return new GoogleAnalyticsIntegration();
      case 'K':
        verify(key, KahunaIntegration.KAHUNA_KEY);
        return new KahunaIntegration();
      case 'L':
        switch (key.charAt(1)) {
          case 'e':
            verify(key, LeanplumIntegration.LEANPLUM_KEY);
            return new LeanplumIntegration();
          case 'o':
            verify(key, LocalyticsIntegration.LOCALYTICS_KEY);
            return new LocalyticsIntegration();
          default:
            break;
        }
      case 'M':
        verify(key, MixpanelIntegration.MIXPANEL_KEY);
        return new MixpanelIntegration();
      case 'Q':
        verify(key, QuantcastIntegration.QUANTCAST_KEY);
        return new QuantcastIntegration();
      case 'T':
        verify(key, TapstreamIntegration.TAPSTREAM_KEY);
        return new TapstreamIntegration();
      default:
        break;
    }
    // this will only be called for bundled integrations, so should fail if we see some unknown
    // bundled integration!
    throw new AssertionError("unknown integration key: " + key);
  }

  private void verify(String actual, String expected) {
    // todo: ideally we wouldn't even have to compare the first n characters that were matched in
    // the trie, but this is ok for now
    if (actual.compareTo(expected) != 0) {
      throw new AssertionError("unknown integration key: " + actual);
    }
  }

  void flush() {
    dispatchOperation(new FlushOperation());
  }

  void dispatchOperation(IntegrationOperation operation) {
    integrationManagerHandler.sendMessage(
        integrationManagerHandler.obtainMessage(IntegrationHandler.DISPATCH_OPERATION, operation));
  }

  void performOperation(IntegrationOperation operation) {
    operation.run(segmentIntegration);

    if (initialized) {
      run(operation);
    } else {
      if (operationQueue == null) {
        operationQueue = new ArrayDeque<IntegrationOperation>();
      }
      logger.debug(OWNER_INTEGRATION_MANAGER, Logger.VERB_ENQUEUE, operation.id(), null);
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
        integrationManagerHandler.obtainMessage(IntegrationHandler.REGISTER_LISTENER, listener));
  }

  void performRegisterIntegrationInitializedListener(OnIntegrationReadyListener listener) {
    this.listener = listener;
    if (initialized && listener != null) {
      // Integrations are already ready, notify the listener right away
      for (AbstractIntegration abstractIntegration : integrations) {
        listener.onIntegrationReady(abstractIntegration.key(),
            abstractIntegration.getUnderlyingInstance());
      }
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
  }

  private static class NetworkingHandler extends Handler {
    static final int FETCH_SETTINGS = 1;

    private final IntegrationManager integrationManager;

    NetworkingHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case FETCH_SETTINGS:
          integrationManager.performFetch();
          break;
        default:
          panic("Unhandled dispatcher message." + msg.what);
      }
    }
  }

  private static class IntegrationHandler extends Handler {
    static final int INITIALIZE = 1;
    static final int DISPATCH_OPERATION = 2;
    static final int REGISTER_LISTENER = 3;

    private final IntegrationManager integrationManager;

    IntegrationHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case INITIALIZE:
          integrationManager.performInitialize((ProjectSettings) msg.obj);
          break;
        case DISPATCH_OPERATION:
          integrationManager.performOperation((IntegrationOperation) msg.obj);
          break;
        case REGISTER_LISTENER:
          integrationManager.performRegisterIntegrationInitializedListener(
              (OnIntegrationReadyListener) msg.obj);
          break;
        default:
          panic("Unhandled dispatcher message." + msg.what);
      }
    }
  }
}
