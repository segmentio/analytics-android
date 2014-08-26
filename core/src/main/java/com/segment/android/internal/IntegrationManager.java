package com.segment.android.internal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  // A set of integrations available on the device
  private final Set<Integration> bundledIntegrations = new HashSet<Integration>();
  // Same as above but explicitly used only to generate server integrations, hence with String keys
  private Map<String, Boolean> serverIntegrations = new LinkedHashMap<String, Boolean>();
  // A set of integrations that are available and have been enabled for this project.
  private final Map<Integration, AbstractIntegration> enabledIntegrations =
      new LinkedHashMap<Integration, AbstractIntegration>();
  private Queue<BasePayload> payloadQueue = new ArrayDeque<BasePayload>();
  private Queue<ActivityLifecyclePayload> activityLifecyclePayloadQueue =
      new ArrayDeque<ActivityLifecyclePayload>();
  boolean initialized;

  enum ActivityLifecycleEvent {
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
  final ExecutorService service;

  public IntegrationManager(Context context, Handler mainThreadHandler,
      SegmentHTTPApi segmentHTTPApi) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.mainThreadHandler = mainThreadHandler;
    this.service = Executors.newSingleThreadExecutor();
    // Look up all the integrations available on the device. This is done early so that we can
    // disable sending to these integrations from the server and properly fill the payloads.
    for (Integration integration : Integration.values()) {
      Logger.d("Checking for integration %s", integration.key());
      try {
        Class.forName(integration.className());
        bundledIntegrations.add(integration);
        serverIntegrations.put(integration.key(), false);
        Logger.d("Loaded integration %s", integration.key());
      } catch (ClassNotFoundException e) {
        Logger.d("Integration %s not bundled", integration.key());
      }
      Logger.d("debug");
    }
    serverIntegrations =
        Collections.unmodifiableMap(serverIntegrations); // don't allow any more modifications
    initialized = false;
    service.submit(new Runnable() {
      @Override public void run() {
        performFetch();
      }
    });
  }

  void performFetch() {
    try {
      if (isConnected(context)) {
        final ProjectSettings projectSettings = segmentHTTPApi.fetchSettings();
        Segment.HANDLER.post(new Runnable() {
          @Override public void run() {
            // todo : does this need to be on the main thread?
            initialize(projectSettings);
          }
        });
      }
    } catch (IOException e) {
      Logger.e(e, "Failed to fetch settings");
      performFetch(); // todo: terminate retry
    }
  }

  private void initialize(ProjectSettings projectSettings) {
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
      Logger.d("Initialized integration %s", integration.key());
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Could not initialize integration %s", integration.key());
    }
  }

  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    if (!initialized) {
      activityLifecyclePayloadQueue.add(
          new ActivityLifecyclePayload(ActivityLifecycleEvent.CREATED, activity, savedInstanceState)
      );
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityCreated(activity, savedInstanceState);
    }
  }

  void onActivityStarted(Activity activity) {
    if (!initialized) {
      activityLifecyclePayloadQueue.add(
          new ActivityLifecyclePayload(ActivityLifecycleEvent.STARTED, activity, null));
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityStarted(activity);
    }
  }

  void onActivityResumed(Activity activity) {
    if (!initialized) {
      activityLifecyclePayloadQueue.add(
          new ActivityLifecyclePayload(ActivityLifecycleEvent.RESUMED, activity, null));
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityResumed(activity);
    }
  }

  void onActivityPaused(Activity activity) {
    if (!initialized) {
      activityLifecyclePayloadQueue.add(
          new ActivityLifecyclePayload(ActivityLifecycleEvent.PAUSED, activity, null));
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityPaused(activity);
    }
  }

  void onActivityStopped(Activity activity) {
    if (!initialized) {
      activityLifecyclePayloadQueue.add(
          new ActivityLifecyclePayload(ActivityLifecycleEvent.STOPPED, activity, null));
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityStopped(activity);
    }
  }

  void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    if (!initialized) {
      activityLifecyclePayloadQueue.add(
          new ActivityLifecyclePayload(ActivityLifecycleEvent.SAVE_INSTANCE, activity, outState));
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivitySaveInstanceState(activity, outState);
    }
  }

  void onActivityDestroyed(Activity activity) {
    if (!initialized) {
      activityLifecyclePayloadQueue.add(
          new ActivityLifecyclePayload(ActivityLifecycleEvent.DESTROYED, activity, null));
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityDestroyed(activity);
    }
  }

  // Analytics Actions
  void identify(IdentifyPayload identify) {
    if (!initialized) {
      payloadQueue.add(identify);
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      if (isBundledIntegrationEnabledForPayload(identify, integration)) {
        integration.identify(identify);
      }
    }
  }

  void group(GroupPayload group) {
    if (!initialized) {
      payloadQueue.add(group);
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      if (isBundledIntegrationEnabledForPayload(group, integration)) {
        integration.group(group);
      }
    }
  }

  public void track(TrackPayload track) {
    if (!initialized) {
      payloadQueue.add(track);
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      if (isBundledIntegrationEnabledForPayload(track, integration)) {
        integration.track(track);
      }
    }
  }

  void alias(AliasPayload alias) {
    if (!initialized) {
      payloadQueue.add(alias);
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      if (isBundledIntegrationEnabledForPayload(alias, integration)) {
        integration.alias(alias);
      }
    }
  }

  void screen(ScreenPayload screen) {
    if (!initialized) {
      payloadQueue.add(screen);
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.screen(screen);
      if (isBundledIntegrationEnabledForPayload(screen, integration)) {
        integration.screen(screen);
      }
    }
  }

  public void flush() {
    if (!initialized) {
      return;
    }
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.flush();
    }
  }

  void replay() {
    for (ActivityLifecyclePayload payload : activityLifecyclePayloadQueue) {
      switch (payload.type) {
        case CREATED:
          if (payload.activityWeakReference.get() != null) {
            onActivityCreated(payload.activityWeakReference.get(), payload.bundle);
          }
          break;
        case STARTED:
          if (payload.activityWeakReference.get() != null) {
            onActivityStarted(payload.activityWeakReference.get());
          }
          break;
        case RESUMED:
          if (payload.activityWeakReference.get() != null) {
            onActivityResumed(payload.activityWeakReference.get());
          }
          break;
        case PAUSED:
          if (payload.activityWeakReference.get() != null) {
            onActivityPaused(payload.activityWeakReference.get());
          }
          break;
        case STOPPED:
          if (payload.activityWeakReference.get() != null) {
            onActivityStopped(payload.activityWeakReference.get());
          }
          break;
        case SAVE_INSTANCE:
          if (payload.activityWeakReference.get() != null) {
            onActivitySaveInstanceState(payload.activityWeakReference.get(), payload.bundle);
          }
          break;
        case DESTROYED:
          if (payload.activityWeakReference.get() != null) {
            onActivityDestroyed(payload.activityWeakReference.get());
          }
          break;
        default:
          throw new IllegalArgumentException("Unknown payload type!" + payload.type);
      }
    }
    Logger.d("Replayed %s activity lifecycle events.", activityLifecyclePayloadQueue.size());
    activityLifecyclePayloadQueue.clear();
    activityLifecyclePayloadQueue = null;

    for (BasePayload payload : payloadQueue) {
      switch (payload.type()) {
        case alias:
          alias((AliasPayload) payload);
          break;
        case group:
          group((GroupPayload) payload);
          break;
        case identify:
          identify((IdentifyPayload) payload);
          break;
        case page:
          screen((ScreenPayload) payload);
          break;
        case screen:
          screen((ScreenPayload) payload);
          break;
        case track:
          track((TrackPayload) payload);
          break;
        default:
          throw new IllegalArgumentException("");
      }
    }
    Logger.d("Replayed %s analytics events.", payloadQueue.size());
    payloadQueue.clear();
    payloadQueue = null;
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
}
