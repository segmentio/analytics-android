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
import java.util.Collections;
import java.util.HashMap;
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
import static com.segment.analytics.Utils.debug;
import static com.segment.analytics.Utils.error;
import static com.segment.analytics.Utils.getSharedPreferences;
import static com.segment.analytics.Utils.isConnected;
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
  final List<AbstractIntegration> integrations;
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

  private IntegrationManager(Context context, SegmentHTTPApi segmentHTTPApi,
      StringCache projectSettingsCache, Stats stats, int queueSize, int flushInterval, String tag,
      boolean debuggingEnabled) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.stats = stats;
    this.debuggingEnabled = debuggingEnabled;
    integrationManagerThread = new HandlerThread(MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    integrationManagerThread.start();
    handler = new IntegrationManagerHandler(integrationManagerThread.getLooper(), this);
    integrations = new LinkedList<AbstractIntegration>();
    bundledIntegrations = Collections.synchronizedMap(new HashMap<String, Boolean>());

    segmentIntegration =
        SegmentIntegration.create(context, queueSize, flushInterval, segmentHTTPApi,
            bundledIntegrations, tag, stats, debuggingEnabled);

    // Look up all the integrations available on the device. This is done early so that we can
    // disable sending to these integrations from the server and properly fill the payloads with
    // this information
    if (isOnClassPath("com.amplitude.api.Amplitude")) {
      bundleIntegration(new AmplitudeIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.appsflyer.AppsFlyerLib")) {
      bundleIntegration(new AppsFlyerIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.bugsnag.android.Bugsnag")) {
      bundleIntegration(new BugsnagIntegration(debuggingEnabled));
    }
    if (isOnClassPath("ly.count.android.api.Countly")) {
      bundleIntegration(new CountlyIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.crittercism.app.Crittercism")) {
      bundleIntegration(new CrittercismIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.flurry.android.FlurryAgent")) {
      bundleIntegration(new FlurryIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.google.android.gms.analytics.GoogleAnalytics")) {
      bundleIntegration(new GoogleAnalyticsIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.localytics.android.LocalyticsSession")) {
      bundleIntegration(new LocalyticsIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.leanplum.Leanplum")) {
      bundleIntegration(new LeanplumIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.mixpanel.android.mpmetrics.MixpanelAPI")) {
      bundleIntegration(new MixpanelIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.quantcast.measurement.service.QuantcastClient")) {
      bundleIntegration(new QuantcastIntegration(debuggingEnabled));
    }
    if (isOnClassPath("com.tapstream.sdk.Tapstream")) {
      bundleIntegration(new TapstreamIntegration(debuggingEnabled));
    }

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

  void bundleIntegration(AbstractIntegration abstractIntegration) {
    bundledIntegrations.put(abstractIntegration.key(), false);
    integrations.add(abstractIntegration);
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
          debug(OWNER_INTEGRATION_MANAGER, "request", "fetch settings", null);
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
        error(OWNER_INTEGRATION_MANAGER, "request", "fetch settings", e, null);
      }
      retryFetch();
    }
  }

  synchronized void initializeIntegrations(ProjectSettings projectSettings) {
    Iterator<AbstractIntegration> iterator = integrations.iterator();
    while (iterator.hasNext()) {
      final AbstractIntegration integration = iterator.next();
      if (projectSettings.containsKey(integration.key())) {
        JsonMap settings = new JsonMap(projectSettings.getJsonMap(integration.key()));
        try {
          integration.initialize(context, settings);
          if (debuggingEnabled) {
            debug(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, integration.key(),
                settings.toString());
          }
          if (listener != null) {
            listener.onIntegrationReady(integration.key(), integration.getUnderlyingInstance());
          }
        } catch (InvalidConfigurationException e) {
          bundledIntegrations.remove(integration.key());
          iterator.remove();
          if (debuggingEnabled) {
            error(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, integration.key(), e,
                settings.toString());
          }
        }
      } else {
        iterator.remove();
        if (debuggingEnabled) {
          debug(OWNER_INTEGRATION_MANAGER, VERB_SKIP, integration.key(),
              "not enabled in project settings: " + projectSettings.keySet());
        }
      }
    }
    Iterator<IntegrationOperation> operationIterator = operationQueue.iterator();
    while (operationIterator.hasNext()) {
      run(operationIterator.next());
      operationIterator.remove();
    }
    operationQueue = null;
    initialized = true;
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
          if (debuggingEnabled) {
            debug(OWNER_INTEGRATION_MANAGER, VERB_ENQUEUE, operation.id(), null);
          }
          if (operationQueue == null) {
            operationQueue = new ArrayDeque<IntegrationOperation>();
          }
          operationQueue.add(operation);
        }
      }
    }
  }

  private void run(IntegrationOperation operation) {
    for (AbstractIntegration integration : integrations) {
      long startTime = System.currentTimeMillis();
      operation.run(integration);
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;
      if (debuggingEnabled) {
        debug(integration.key(), VERB_DISPATCH, operation.id(),
            String.format("duration: %s", duration));
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
