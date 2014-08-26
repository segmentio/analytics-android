package com.segment.android.internal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.android.Integration;
import com.segment.android.Segment;
import com.segment.android.internal.integrations.AbstractIntegration;
import com.segment.android.internal.integrations.AmplitudeIntegration;
import com.segment.android.internal.integrations.BugsnagIntegration;
import com.segment.android.internal.integrations.CountlyIntegration;
import com.segment.android.internal.integrations.CrittercismIntegration;
import com.segment.android.internal.integrations.FlurryIntegration;
import com.segment.android.internal.integrations.GoogleAnalyticsIntegration;
import com.segment.android.internal.integrations.InvalidConfigurationException;
import com.segment.android.internal.integrations.LocalyticsIntegration;
import com.segment.android.internal.integrations.MixpanelIntegration;
import com.segment.android.internal.integrations.QuantcastIntegration;
import com.segment.android.internal.integrations.TapstreamIntegration;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.android.internal.Utils.isConnected;

/**
 * Manages bundled integrations. This class will maintain it's own queue for events to account for
 * the latency between receiving the first event, fetching remote settings and enabling the
 * integrations. Once we enable all integrations - we'll replay any events on disk. This should
 * only affect the first app install, subsequent launches will be use a cached value from disk.
 * Note  that none of the activity lifecycle events are queued to disk, we only have a memory queue
 * for them.
 */
public class IntegrationManager {
  static final int REQUEST_LOAD = 0;
  static final int REQUEST_FETCH_SETTINGS = 1;
  static final int REQUEST_INIT = 2;
  static final int REQUEST_LIFECYCLE_EVENT = 3;
  static final int REQUEST_ANALYTICS_EVENT = 4;
  static final int REQUEST_FLUSH = 5;

  private static final String INTEGRATION_MANAGER_THREAD_NAME =
      Utils.THREAD_PREFIX + "IntegrationManager";
  // A set of integrations available on the device
  private final Set<Integration> bundledIntegrations = new HashSet<Integration>();
  // Same as above but explicitly used only to generate server integrations, hence with String keys
  private Map<String, Boolean> serverIntegrations = new LinkedHashMap<String, Boolean>();
  // A set of integrations that are available and have been enabled for this project.
  private final Map<Integration, AbstractIntegration> enabledIntegrations =
      new LinkedHashMap<Integration, AbstractIntegration>();
  private Queue<IntegrationOperation> operationQueue = new ArrayDeque<IntegrationOperation>();
  boolean initialized;

  public enum ActivityLifecycleEvent {
    CREATED, STARTED, RESUMED, PAUSED, STOPPED, SAVE_INSTANCE, DESTROYED
  }

  static class ActivityLifecyclePayload {
    final ActivityLifecycleEvent type;
    final WeakReference<Activity> activityWeakReference;
    final Bundle bundle;

    ActivityLifecyclePayload(ActivityLifecycleEvent type, Activity activity, Bundle bundle) {
      this.type = type;
      this.activityWeakReference = new WeakReference<Activity>(activity);
      this.bundle = bundle;
    }
  }

  final Context context;
  final SegmentHTTPApi segmentHTTPApi;
  final Handler mainThreadHandler;
  final HandlerThread integrationManagerThread;
  final Handler handler;
  final Stats stats;

  public IntegrationManager(Context context, Handler mainThreadHandler,
      SegmentHTTPApi segmentHTTPApi, Stats stats) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.mainThreadHandler = mainThreadHandler;
    this.stats = stats;
    integrationManagerThread =
        new HandlerThread(INTEGRATION_MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    integrationManagerThread.start();
    handler = new IntegrationManagerHandler(integrationManagerThread.getLooper(), this);

    dispatchLoad();
    dispatchFetch();
  }

  void dispatchLoad() {
    handler.sendMessage(handler.obtainMessage(REQUEST_LOAD));
  }

  void performLoad() {
    // Look up all the integrations available on the device. This is done early so that we can
    // disable sending to these integrations from the server and properly fill the payloads.
    for (Integration integration : Integration.values()) {
      Logger.v("Checking for integration %s", integration.key());
      try {
        Class.forName(integration.className());
        bundledIntegrations.add(integration);
        serverIntegrations.put(integration.key(), false);
        Logger.v("Loaded integration %s", integration.key());
      } catch (ClassNotFoundException e) {
        Logger.v("Integration %s not bundled", integration.key());
      }
    }
    serverIntegrations =
        Collections.unmodifiableMap(serverIntegrations); // don't allow any more modifications
    initialized = false;
  }

  void dispatchFetch() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FETCH_SETTINGS));
  }

  void performFetch() {
    try {
      if (isConnected(context)) {
        dispatchInit(segmentHTTPApi.fetchSettings());
      }
    } catch (IOException e) {
      Logger.e(e, "Failed to fetch settings. Retrying.");
      dispatchFetch();
    }
  }

  void dispatchInit(ProjectSettings projectSettings) {
    handler.sendMessage(handler.obtainMessage(REQUEST_INIT, projectSettings));
  }

  void performInit(ProjectSettings projectSettings) {
    for (Integration integration : bundledIntegrations) {
      if (projectSettings.containsKey(integration.key())) {
        JsonMap settings = new JsonMap(projectSettings.getJsonMap(integration.key()));
        switch (integration) {
          case AMPLITUDE:
            enableIntegration(Integration.AMPLITUDE, new AmplitudeIntegration(), settings);
            break;
          case BUGSNAG:
            enableIntegration(Integration.BUGSNAG, new BugsnagIntegration(), settings);
            break;
          case COUNTLY:
            enableIntegration(Integration.COUNTLY, new CountlyIntegration(), settings);
            break;
          case CRITTERCISM:
            enableIntegration(Integration.CRITTERCISM, new CrittercismIntegration(), settings);
            break;
          case FLURRY:
            enableIntegration(Integration.FLURRY, new FlurryIntegration(), settings);
            break;
          case GOOGLE_ANALYTICS:
            enableIntegration(Integration.GOOGLE_ANALYTICS, new GoogleAnalyticsIntegration(),
                settings);
            break;
          case LOCALYTICS:
            enableIntegration(Integration.LOCALYTICS, new LocalyticsIntegration(), settings);
            break;
          case MIXPANEL:
            enableIntegration(Integration.MIXPANEL, new MixpanelIntegration(), settings);
            break;
          case QUANTCAST:
            enableIntegration(Integration.QUANTCAST, new QuantcastIntegration(), settings);
            break;
          case TAPSTREAM:
            enableIntegration(Integration.TAPSTREAM, new TapstreamIntegration(), settings);
            break;
          default:
            throw new IllegalArgumentException("Unknown integration! " + integration.key());
        }
      }
    }
    initialized = true;
    replay();
  }

  private void enableIntegration(Integration integration, AbstractIntegration abstractIntegration,
      JsonMap settings) {
    try {
      abstractIntegration.initialize(context, settings);
      enabledIntegrations.put(integration, abstractIntegration);
      Logger.v("Initialized integration %s", integration.key());
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Could not initialize integration %s", integration.key());
    }
  }

  // Activity Lifecycle Events
  public void dispatchLifecycleEvent(ActivityLifecycleEvent event, Activity activity,
      Bundle bundle) {
    handler.sendMessage(handler.obtainMessage(REQUEST_LIFECYCLE_EVENT,
        new ActivityLifecyclePayload(event, activity, bundle)));
  }

  void enqueue(final ActivityLifecyclePayload payload) {
    switch (payload.type) {
      case CREATED:
        if (payload.activityWeakReference.get() != null) {
          enqueue(new IntegrationOperation() {
            @Override public void run(AbstractIntegration integration) {
              integration.onActivityCreated(payload.activityWeakReference.get(), payload.bundle);
            }
          });
        }
        break;
      case STARTED:
        if (payload.activityWeakReference.get() != null) {
          enqueue(new IntegrationOperation() {
            @Override public void run(AbstractIntegration integration) {
              integration.onActivityStarted(payload.activityWeakReference.get());
            }
          });
        }
        break;
      case RESUMED:
        if (payload.activityWeakReference.get() != null) {
          enqueue(new IntegrationOperation() {
            @Override public void run(AbstractIntegration integration) {
              integration.onActivityResumed(payload.activityWeakReference.get());
            }
          });
        }
        break;
      case PAUSED:
        if (payload.activityWeakReference.get() != null) {
          enqueue(new IntegrationOperation() {
            @Override public void run(AbstractIntegration integration) {
              integration.onActivityPaused(payload.activityWeakReference.get());
            }
          });
        }
        break;
      case STOPPED:
        if (payload.activityWeakReference.get() != null) {
          enqueue(new IntegrationOperation() {
            @Override public void run(AbstractIntegration integration) {
              integration.onActivityStopped(payload.activityWeakReference.get());
            }
          });
        }
        break;
      case SAVE_INSTANCE:
        if (payload.activityWeakReference.get() != null) {
          enqueue(new IntegrationOperation() {
            @Override public void run(AbstractIntegration integration) {
              integration.onActivitySaveInstanceState(payload.activityWeakReference.get(),
                  payload.bundle);
            }
          });
        }
        break;
      case DESTROYED:
        if (payload.activityWeakReference.get() != null) {
          enqueue(new IntegrationOperation() {
            @Override public void run(AbstractIntegration integration) {
              integration.onActivityDestroyed(payload.activityWeakReference.get());
            }
          });
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown payload type!" + payload.type);
    }
  }

  public void dispatchAnalyticsEvent(BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_ANALYTICS_EVENT, payload));
  }

  void enqueue(final BasePayload payload) {
    switch (payload.type()) {
      case alias:
        enqueue(new IntegrationOperation() {
          @Override public void run(AbstractIntegration integration) {
            if (isBundledIntegrationEnabledForPayload(payload, integration)) {
              integration.alias((AliasPayload) payload);
            }
          }
        });
        break;
      case group:
        enqueue(new IntegrationOperation() {
          @Override public void run(AbstractIntegration integration) {
            if (isBundledIntegrationEnabledForPayload(payload, integration)) {
              integration.group((GroupPayload) payload);
            }
          }
        });
        break;
      case identify:
        enqueue(new IntegrationOperation() {
          @Override public void run(AbstractIntegration integration) {
            if (isBundledIntegrationEnabledForPayload(payload, integration)) {

              integration.identify((IdentifyPayload) payload);
            }
          }
        });
        break;
      case page:
      case screen:
        enqueue(new IntegrationOperation() {
          @Override public void run(AbstractIntegration integration) {
            if (isBundledIntegrationEnabledForPayload(payload, integration)) {
              integration.screen((ScreenPayload) payload);
            }
          }
        });
        break;
      case track:
        enqueue(new IntegrationOperation() {
          @Override public void run(AbstractIntegration integration) {
            if (isBundledIntegrationEnabledForPayload(payload, integration)) {
              integration.track((TrackPayload) payload);
            }
          }
        });
        break;
      default:
        throw new IllegalArgumentException("Unknown payload type!" + payload.type());
    }
  }

  public void dispatchFlush() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
  }

  void performFlush() {
    enqueue(new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration) {
        integration.flush();
      }
    });
  }

  private interface IntegrationOperation {
    // todo: enumerate operations to avoid inn
    void run(AbstractIntegration integration);
  }

  void enqueue(IntegrationOperation operation) {
    if (!initialized) {
      Logger.v("Integrations not yet initialized! Queuing operation.");
      operationQueue.add(operation);
    } else {
      run(operation);
    }
  }

  private void run(IntegrationOperation operation) {
    for (Map.Entry<Integration, AbstractIntegration> entry : enabledIntegrations.entrySet()) {
      long startTime = System.currentTimeMillis();
      operation.run(entry.getValue());
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;
      Logger.v("Integration %s took %s ms to run operation", entry.getKey().key(), duration);
      stats.dispatchIntegrationOperation(duration);
    }
  }

  void replay() {
    Logger.v("Replaying %s events.", operationQueue.size());
    while (operationQueue.size() > 0) {
      IntegrationOperation operation = operationQueue.peek();
      run(operation);
      operationQueue.remove();
    }
  }

  private boolean isBundledIntegrationEnabledForPayload(BasePayload payload,
      AbstractIntegration integration) {
    Boolean enabled = true;
    // look in the payload.context.integrations to see which Bundled integrations should be
    // disabled. payload.integrations is reserved for the server, where all bundled integrations
    // have been  set to false
    JsonMap integrations = payload.context().getIntegrations();
    if (!JsonMap.isNullOrEmpty(integrations)) {
      String key = integration.provider().key();
      if (integrations.containsKey(key)) {
        enabled = integrations.getBoolean(key);
      } else if (integrations.containsKey("All")) {
        enabled = integrations.getBoolean("All");
      } else if (integrations.containsKey("all")) {
        enabled = integrations.getBoolean("all");
      }
    }
    return enabled;
  }

  public Object getInstance(Integration integration) {
    if (initialized) {
      AbstractIntegration abstractIntegration = enabledIntegrations.get(integration);
      if (abstractIntegration == null) {
        return Boolean.FALSE;
      }
      Object instance = abstractIntegration.getUnderlyingInstance();
      return instance == null ? Boolean.TRUE : instance;
    } else {
      return Boolean.FALSE;
    }
  }

  public Map<String, Boolean> bundledIntegrations() {
    return serverIntegrations;
  }

  private static class IntegrationManagerHandler extends Handler {
    private final IntegrationManager integrationManager;

    public IntegrationManagerHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_LOAD:
          integrationManager.performLoad();
          break;
        case REQUEST_FETCH_SETTINGS:
          integrationManager.performFetch();
          break;
        case REQUEST_INIT:
          ProjectSettings settings = (ProjectSettings) msg.obj;
          integrationManager.performInit(settings);
          break;
        case REQUEST_LIFECYCLE_EVENT:
          ActivityLifecyclePayload activityLifecyclePayload = (ActivityLifecyclePayload) msg.obj;
          integrationManager.enqueue(activityLifecyclePayload);
          break;
        case REQUEST_ANALYTICS_EVENT:
          BasePayload basePayload = (BasePayload) msg.obj;
          integrationManager.enqueue(basePayload);
          break;
        case REQUEST_FLUSH:
          integrationManager.performFlush();
          break;
        default:
          Segment.HANDLER.post(new Runnable() {
            @Override public void run() {
              throw new AssertionError("Unhandled dispatcher message." + msg.what);
            }
          });
      }
    }
  }
}
