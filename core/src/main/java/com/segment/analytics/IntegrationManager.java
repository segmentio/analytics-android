package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Logger.THREAD_INTEGRATION_MANAGER;
import static com.segment.analytics.Logger.VERB_DISPATCHED;
import static com.segment.analytics.Logger.VERB_DISPATCHING;
import static com.segment.analytics.Logger.VERB_INITIALIZED;
import static com.segment.analytics.Logger.VERB_INITIALIZING;
import static com.segment.analytics.Logger.VERB_SKIPPED;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.isNullOrEmpty;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;

/**
 * Manages bundled integrations. This class will maintain it's own queue for events to account for
 * the latency between receiving the first event, fetching remote settings and enabling the
 * integrations. Once we enable all integrations - we'll replay any events in the queue. This
 * should
 * only affect the first app install, subsequent launches will be use a cached value from disk.
 */
class IntegrationManager {
  private static final String PROJECT_SETTINGS_CACHE_KEY = "project-settings";

  static final int REQUEST_FETCH_SETTINGS = 1;
  static final int REQUEST_INIT = 2;
  static final int REQUEST_LIFECYCLE_EVENT = 3;
  static final int REQUEST_ANALYTICS_EVENT = 4;
  static final int REQUEST_FLUSH = 5;

  private static final String INTEGRATION_MANAGER_THREAD_NAME =
      Utils.THREAD_PREFIX + "IntegrationManager";
  // A set of integrations available on the device
  private final Set<AbstractIntegrationAdapter> bundledIntegrations =
      new HashSet<AbstractIntegrationAdapter>();
  // A map of integrations that were found on the device, so that we disable them for servers
  private Map<String, Boolean> serverIntegrations = new LinkedHashMap<String, Boolean>();
  private Queue<IntegrationOperation> operationQueue = new ArrayDeque<IntegrationOperation>();
  final AtomicBoolean initialized = new AtomicBoolean();

  static class ActivityLifecyclePayload {
    enum Type {
      CREATED, STARTED, RESUMED, PAUSED, STOPPED, SAVE_INSTANCE, DESTROYED
    }

    final Type type;
    final WeakReference<Activity> activityWeakReference;
    final Bundle bundle;
    final String id;

    ActivityLifecyclePayload(Type type, Activity activity, Bundle bundle) {
      this.type = type;
      this.activityWeakReference = new WeakReference<Activity>(activity);
      this.bundle = bundle;
      this.id = UUID.randomUUID().toString();
    }
  }

  interface IntegrationOperation {
    void run(AbstractIntegrationAdapter integration);

    String id();

    String type();
  }

  static class ActivityLifecycleOperation implements IntegrationOperation {
    final ActivityLifecyclePayload payload;
    final Logger logger;

    ActivityLifecycleOperation(ActivityLifecyclePayload payload, Logger logger) {
      this.payload = payload;
      this.logger = logger;
    }

    @Override public void run(AbstractIntegrationAdapter integration) {
      Activity activity = payload.activityWeakReference.get();
      if (activity == null) {
        if (logger.loggingEnabled) {
          logger.debug(THREAD_INTEGRATION_MANAGER, VERB_SKIPPED, payload.id,
              "{type: " + payload.type + '}');
        }
        return;
      }
      switch (payload.type) {
        case CREATED:
          integration.onActivityCreated(activity, payload.bundle);
          break;
        case STARTED:
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
          integration.onActivitySaveInstanceState(activity, payload.bundle);
          break;
        case DESTROYED:
          integration.onActivityDestroyed(activity);
          break;
        default:
          panic("Unknown payload type!" + payload.type);
      }
    }

    @Override public String id() {
      return payload.id;
    }

    @Override public String type() {
      return payload.type.toString();
    }
  }

  static class FlushOperation implements IntegrationOperation {
    final String id;

    FlushOperation() {
      this.id = UUID.randomUUID().toString();
    }

    @Override public void run(AbstractIntegrationAdapter integration) {
      integration.flush();
    }

    @Override public String id() {
      return id;
    }

    @Override public String type() {
      return "flush";
    }
  }

  static class AnalyticsOperation implements IntegrationOperation {
    final BasePayload payload;

    AnalyticsOperation(BasePayload payload) {
      this.payload = payload;
    }

    @Override public void run(AbstractIntegrationAdapter integration) {
      if (!isBundledIntegrationEnabledForPayload(payload, integration)) return;

      switch (payload.type()) {
        case alias:
          integration.alias((AliasPayload) payload);
          break;
        case group:
          integration.group((GroupPayload) payload);
          break;
        case identify:
          integration.identify((IdentifyPayload) payload);
          break;
        case screen:
          integration.screen((ScreenPayload) payload);
          break;
        case track:
          integration.track((TrackPayload) payload);
          break;
        default:
          panic("Unknown payload type!" + payload.type());
      }
    }

    @Override public String id() {
      return payload.messageId();
    }

    @Override public String type() {
      return payload.type().toString();
    }
  }

  static IntegrationManager create(Context context, SegmentHTTPApi segmentHTTPApi, Stats stats,
      Logger logger) {
    StringCache projectSettingsCache =
        new StringCache(Utils.getSharedPreferences(context), PROJECT_SETTINGS_CACHE_KEY);
    return new IntegrationManager(context, segmentHTTPApi, projectSettingsCache, stats, logger);
  }

  final Context context;
  final SegmentHTTPApi segmentHTTPApi;
  final HandlerThread integrationManagerThread;
  final Handler handler;
  final Stats stats;
  final Logger logger;
  final StringCache projectSettingsCache;

  private IntegrationManager(Context context, SegmentHTTPApi segmentHTTPApi,
      StringCache projectSettingsCache, Stats stats, Logger logger) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.stats = stats;
    this.logger = logger;
    integrationManagerThread =
        new HandlerThread(INTEGRATION_MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    integrationManagerThread.start();
    handler = new IntegrationManagerHandler(integrationManagerThread.getLooper(), this);

    loadIntegrations();

    this.projectSettingsCache = projectSettingsCache;
    ProjectSettings projectSettings = ProjectSettings.load(projectSettingsCache);
    if (projectSettings == null) {
      dispatchFetch();
    } else {
      dispatchInit(projectSettings);
      // todo: stash staleness factor in a constant
      if (projectSettings.timestamp() + 10800000L < System.currentTimeMillis()) {
        dispatchFetch();
      }
    }
  }

  void loadIntegrations() {
    initialized.set(false);
    // Look up all the integrations available on the device. This is done early so that we can
    // disable sending to these integrations from the server and properly fill the payloads.
    addToBundledIntegrations(new AmplitudeIntegrationAdapter());
    addToBundledIntegrations(new BugsnagIntegrationAdapter());
    addToBundledIntegrations(new CountlyIntegrationAdapter());
    addToBundledIntegrations(new CrittercismIntegrationAdapter());
    addToBundledIntegrations(new FlurryIntegrationAdapter());
    addToBundledIntegrations(new GoogleAnalyticsIntegrationAdapter());
    addToBundledIntegrations(new LocalyticsIntegrationAdapter());
    addToBundledIntegrations(new MixpanelIntegrationAdapter());
    addToBundledIntegrations(new QuantcastIntegrationAdapter());
    addToBundledIntegrations(new TapstreamIntegrationAdapter());
  }

  void addToBundledIntegrations(AbstractIntegrationAdapter abstractIntegrationAdapter) {
    try {
      Class.forName(abstractIntegrationAdapter.className());
      bundledIntegrations.add(abstractIntegrationAdapter);
      serverIntegrations.put(abstractIntegrationAdapter.key(), false);
    } catch (ClassNotFoundException e) {
      // ignored
    }
  }

  void dispatchFetch() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FETCH_SETTINGS));
  }

  void performFetch() {
    if (logger.loggingEnabled) {
      logger.debug(THREAD_INTEGRATION_MANAGER, VERB_DISPATCHING, "fetch settings", null);
    }
    try {
      if (isConnected(context)) {
        ProjectSettings projectSettings = segmentHTTPApi.fetchSettings();
        if (logger.loggingEnabled) {
          logger.debug(THREAD_INTEGRATION_MANAGER, VERB_DISPATCHED, "fetch settings", null);
        }
        performInit(projectSettings);
      } else {
        // re-schedule in a minute, todo: move to constant, same as below
        handler.sendMessageDelayed(handler.obtainMessage(REQUEST_FETCH_SETTINGS), 1000 * 60);
        if (logger.loggingEnabled) {
          logger.debug(THREAD_INTEGRATION_MANAGER, VERB_SKIPPED, "fetch settings", null);
        }
      }
    } catch (IOException e) {
      if (logger.loggingEnabled) {
        logger.error(THREAD_INTEGRATION_MANAGER, VERB_DISPATCHING, "fetch settings", e, null);
      }
      // re-schedule in a minute, todo: move to constant
      handler.sendMessageDelayed(handler.obtainMessage(REQUEST_FETCH_SETTINGS), 1000 * 60);
    }
  }

  void dispatchInit(ProjectSettings projectSettings) {
    handler.sendMessage(handler.obtainMessage(REQUEST_INIT, projectSettings));
  }

  void performInit(ProjectSettings projectSettings) {
    String projectSettingsJson = projectSettings.toString();
    projectSettingsCache.set(projectSettingsJson);

    if (initialized.get()) return; // skip if already initialized

    Iterator<AbstractIntegrationAdapter> iterator = bundledIntegrations.iterator();
    while (iterator.hasNext()) {
      AbstractIntegrationAdapter integration = iterator.next();
      if (projectSettings.containsKey(integration.key())) {
        JsonMap settings = new JsonMap(projectSettings.getJsonMap(integration.key()));
        try {
          integration.initialize(context, settings);
          if (logger.loggingEnabled) {
            logger.debug(THREAD_INTEGRATION_MANAGER, VERB_INITIALIZED, integration.key(),
                settings.toString());
          }
        } catch (InvalidConfigurationException e) {
          iterator.remove();
          if (logger.loggingEnabled) {
            logger.error(THREAD_INTEGRATION_MANAGER, VERB_INITIALIZING, integration.key(), e,
                settings.toString());
          }
        }
      } else {
        iterator.remove();
        if (logger.loggingEnabled) {
          logger.debug(THREAD_INTEGRATION_MANAGER, VERB_SKIPPED, integration.key(), null);
        }
      }
    }
    initialized.set(true);
    replay();
  }

  void dispatch(ActivityLifecyclePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_LIFECYCLE_EVENT, payload));
  }

  void performEnqueue(ActivityLifecyclePayload payload) {
    ActivityLifecycleOperation operation = new ActivityLifecycleOperation(payload, logger);
    enqueue(operation);
  }

  void dispatch(BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_ANALYTICS_EVENT, payload));
  }

  void performEnqueue(BasePayload payload) {
    enqueue(new AnalyticsOperation(payload));
  }

  void dispatchFlush() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
  }

  void performFlush() {
    enqueue(new FlushOperation());
  }

  private void enqueue(IntegrationOperation operation) {
    if (!initialized.get()) {
      operationQueue.add(operation);
    } else {
      run(operation);
    }
  }

  private void run(IntegrationOperation operation) {
    for (AbstractIntegrationAdapter integration : bundledIntegrations) {
      long startTime = System.currentTimeMillis();
      operation.run(integration);
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;
      if (logger.loggingEnabled) {
        logger.debug(THREAD_INTEGRATION_MANAGER, VERB_DISPATCHED, operation.id(),
            String.format("{integration: %s, type: %s, duration: %s}", integration.key(),
                operation.type(), duration)
        );
      }
      stats.dispatchIntegrationOperation(duration);
    }
  }

  void replay() {
    for (IntegrationOperation operation : operationQueue) {
      run(operation);
    }
    operationQueue.clear();
    operationQueue = null;
  }

  private static boolean isBundledIntegrationEnabledForPayload(BasePayload payload,
      AbstractIntegrationAdapter integration) {
    boolean enabled = true;
    // look in the payload.context.integrations to see which Bundled integrations should be
    // disabled. payload.integrations is reserved for the server, where all bundled integrations
    // have been  set to false
    JsonMap integrations = payload.context().getIntegrations();
    if (!isNullOrEmpty(integrations)) {
      String key = integration.key();
      if (integrations.containsKey(key)) {
        enabled = integrations.getBoolean(key, true);
      } else if (integrations.containsKey("All")) {
        enabled = integrations.getBoolean("All", true);
      } else if (integrations.containsKey("all")) {
        enabled = integrations.getBoolean("all", true);
      }
    }
    return enabled;
  }

  Map<String, Boolean> bundledIntegrations() {
    return serverIntegrations;
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
        case REQUEST_INIT:
          integrationManager.performInit((ProjectSettings) msg.obj);
          break;
        case REQUEST_LIFECYCLE_EVENT:
          ActivityLifecyclePayload activityLifecyclePayload = (ActivityLifecyclePayload) msg.obj;
          integrationManager.performEnqueue(activityLifecyclePayload);
          break;
        case REQUEST_ANALYTICS_EVENT:
          BasePayload basePayload = (BasePayload) msg.obj;
          integrationManager.performEnqueue(basePayload);
          break;
        case REQUEST_FLUSH:
          integrationManager.performFlush();
          break;
        default:
          Analytics.MAIN_LOOPER.post(new Runnable() {
            @Override public void run() {
              throw new AssertionError("Unhandled dispatcher message." + msg.what);
            }
          });
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
}
