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
import static com.segment.analytics.Utils.OWNER_INTEGRATION_MANAGER;
import static com.segment.analytics.Utils.THREAD_PREFIX;
import static com.segment.analytics.Utils.VERB_DISPATCH;
import static com.segment.analytics.Utils.VERB_ENQUEUE;
import static com.segment.analytics.Utils.VERB_INITIALIZE;
import static com.segment.analytics.Utils.VERB_SKIP;
import static com.segment.analytics.Utils.createMap;
import static com.segment.analytics.Utils.debug;
import static com.segment.analytics.Utils.error;
import static com.segment.analytics.Utils.getSharedPreferences;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.isNullOrEmpty;
import static com.segment.analytics.Utils.isOnClassPath;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;

/**
 * Manages bundled integrations. This class will maintain it's own queue for events to account for
 * the latency between receiving the first event, fetching remote settings and enabling the
 * integrations. Once we enable all integrations - we'll replay any events in the queue. This will
 * only affect the first app install, subsequent launches will be use the cached settings on disk.
 */
class IntegrationManager {
  static final int REQUEST_FETCH_SETTINGS = 1;

  private static final String PROJECT_SETTINGS_CACHE_KEY = "project-settings";
  private static final String MANAGER_THREAD_NAME = THREAD_PREFIX + "IntegrationManager";
  private static final long SETTINGS_REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // 24 hours
  private static final long SETTINGS_ERROR_INTERVAL = 1000 * 60; // 1 minute

  final Context context;
  final SegmentHTTPApi segmentHTTPApi;
  final HandlerThread integrationManagerThread;
  final Handler handler;
  final Stats stats;
  final boolean debuggingEnabled;
  final StringCache projectSettingsCache;
  final SegmentIntegration segmentIntegration;
  List<AbstractIntegration> integrations;
  final Map<String, Boolean> bundledIntegrations;
  Queue<IntegrationOperation> operationQueue;
  volatile boolean initialized;
  OnIntegrationReadyListener listener;

  static IntegrationManager create(Context context, SegmentHTTPApi segmentHTTPApi, Stats stats,
      int queueSize, int flushInterval, String tag, boolean debuggingEnabled) {
    StringCache projectSettingsCache =
        new StringCache(getSharedPreferences(context), PROJECT_SETTINGS_CACHE_KEY);
    return new IntegrationManager(context, segmentHTTPApi, projectSettingsCache, stats, queueSize,
        flushInterval, tag, debuggingEnabled);
  }

  IntegrationManager(Context context, SegmentHTTPApi segmentHTTPApi,
      StringCache projectSettingsCache, Stats stats, int queueSize, int flushInterval, String tag,
      boolean debuggingEnabled) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.stats = stats;
    this.debuggingEnabled = debuggingEnabled;
    integrationManagerThread = new HandlerThread(MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    integrationManagerThread.start();
    handler = new IntegrationManagerHandler(integrationManagerThread.getLooper(), this);

    // Look up all the integrations available on the device. This is done early so that we can
    // disable sending to these integrations from the server and properly fill the payloads with
    // this information
    bundledIntegrations = createMap();
    findBundledIntegration("com.amplitude.api.Amplitude", AmplitudeIntegration.AMPLITUDE_KEY);
    findBundledIntegration("com.appsflyer.AppsFlyerLib", AppsFlyerIntegration.APPS_FLYER_KEY);
    findBundledIntegration("com.bugsnag.android.Bugsnag", BugsnagIntegration.BUGSNAG_KEY);
    findBundledIntegration("ly.count.android.api.Countly", CountlyIntegration.COUNTLY_KEY);
    findBundledIntegration("com.crittercism.app.Crittercism",
        CrittercismIntegration.CRITTERCISM_KEY);
    findBundledIntegration("com.flurry.android.FlurryAgent", FlurryIntegration.FLURRY_KEY);
    findBundledIntegration("com.google.android.gms.analytics.GoogleAnalytics",
        GoogleAnalyticsIntegration.GOOGLE_ANALYTICS_KEY);
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
            bundledIntegrations, tag, stats, debuggingEnabled);

    this.projectSettingsCache = projectSettingsCache;
    ProjectSettings projectSettings = ProjectSettings.load(projectSettingsCache);
    if (projectSettings == null) {
      dispatchFetch();
    } else {
      initializeIntegrations(projectSettings);
      if (projectSettings.timestamp() + SETTINGS_REFRESH_INTERVAL < System.currentTimeMillis()) {
        dispatchFetch();
      }
    }
  }

  void findBundledIntegration(String className, String key) {
    if (isOnClassPath(className)) bundledIntegrations.put(key, false);
  }

  void dispatchFetch() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FETCH_SETTINGS));
  }

  void retryFetch() {
    handler.sendMessageDelayed(handler.obtainMessage(REQUEST_FETCH_SETTINGS),
        SETTINGS_ERROR_INTERVAL);
  }

  void performFetch() {
    try {
      if (isConnected(context)) {
        if (debuggingEnabled) {
          debug(OWNER_INTEGRATION_MANAGER, "fetch", "settings", null);
        }
        final ProjectSettings projectSettings = segmentHTTPApi.fetchSettings();
        String projectSettingsJson = projectSettings.toString();
        projectSettingsCache.set(projectSettingsJson);
        if (!initialized) {
          // Only initialize integrations if not done already
          Analytics.HANDLER.post(new Runnable() {
            @Override public void run() {
              initializeIntegrations(projectSettings);
            }
          });
        }
      } else {
        retryFetch();
      }
    } catch (IOException e) {
      if (debuggingEnabled) {
        error(OWNER_INTEGRATION_MANAGER, "fetch", "settings", e, null);
      }
      retryFetch();
    }
  }

  synchronized void initializeIntegrations(ProjectSettings projectSettings) {
    integrations = new LinkedList<AbstractIntegration>();
    Iterator<Map.Entry<String, Boolean>> iterator = bundledIntegrations.entrySet().iterator();
    while (iterator.hasNext()) {
      String key = iterator.next().getKey();
      if (projectSettings.containsKey(key)) {
        JsonMap settings = new JsonMap(projectSettings.getJsonMap(key));
        AbstractIntegration integration = createIntegrationForKey(key);
        try {
          if (debuggingEnabled) {
            debug(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, key, "settings: %s", settings);
          }
          integration.initialize(context, settings, debuggingEnabled);
          if (listener != null) {
            listener.onIntegrationReady(key, integration.getUnderlyingInstance());
          }
          integrations.add(integration);
        } catch (InvalidConfigurationException e) {
          iterator.remove();
          if (debuggingEnabled) {
            error(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, integration.key(), e, null);
          }
        }
      } else {
        iterator.remove();
        if (debuggingEnabled) {
          debug(OWNER_INTEGRATION_MANAGER, VERB_SKIP, key, "settings: %s",
              projectSettings.keySet());
        }
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
    // Todo: consume the entire string to verify the key? e.g. Amplitude vs Amhuik (random)
    switch (key.charAt(0)) {
      case 'A':
        switch (key.charAt(1)) {
          case 'm':
            return new AmplitudeIntegration();
          case 'p':
            return new AppsFlyerIntegration();
          default:
            break;
        }
      case 'B':
        return new BugsnagIntegration();
      case 'C':
        switch (key.charAt(1)) {
          case 'o':
            return new CountlyIntegration();
          case 'r':
            return new CrittercismIntegration();
          default:
            break;
        }
      case 'F':
        return new FlurryIntegration();
      case 'G':
        return new GoogleAnalyticsIntegration();
      case 'L':
        switch (key.charAt(1)) {
          case 'e':
            return new LeanplumIntegration();
          case 'o':
            return new LocalyticsIntegration();
          default:
            break;
        }
      case 'M':
        return new MixpanelIntegration();
      case 'Q':
        return new QuantcastIntegration();
      case 'T':
        return new TapstreamIntegration();
      default:
        break;
    }
    throw new AssertionError("unknown integration key: " + key);
  }

  void flush() {
    submit(new FlushOperation());
  }

  void submit(IntegrationOperation operation) {
    operation.run(segmentIntegration);
    if (initialized) {
      run(operation);
    } else {
      // Integrations might be being initialized, so let's wait for the lock
      synchronized (this) {
        if (initialized) {
          run(operation);
        } else {
          if (operationQueue == null) {
            operationQueue = new ArrayDeque<IntegrationOperation>();
          }
          if (debuggingEnabled) {
            debug(OWNER_INTEGRATION_MANAGER, VERB_ENQUEUE, operation.id(), null);
          }
          operationQueue.add(operation);
        }
      }
    }
  }

  void run(IntegrationOperation operation) {
    for (AbstractIntegration integration : integrations) {
      long startTime = System.currentTimeMillis();
      operation.run(integration);
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;
      if (debuggingEnabled) {
        debug(integration.key(), VERB_DISPATCH, operation.id(), "duration: %s", duration);
      }
      stats.dispatchIntegrationOperation(duration);
    }
  }

  void registerIntegrationInitializedListener(OnIntegrationReadyListener listener) {
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
    quitThread(integrationManagerThread);
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

  private static class IntegrationManagerHandler extends Handler {
    private final IntegrationManager integrationManager;

    IntegrationManagerHandler(Looper looper, IntegrationManager integrationManager) {
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
}
