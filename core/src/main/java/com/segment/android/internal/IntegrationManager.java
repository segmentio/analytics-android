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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
      try {
        Class.forName(integration.className());
        bundledIntegrations.add(integration);
        serverIntegrations.put(integration.key(), false);
      } catch (ClassNotFoundException e) {
        Logger.d("%s not bundled", integration.key());
      }
    }
    serverIntegrations =
        Collections.unmodifiableMap(serverIntegrations); // don't allow any more modifications

    service.submit(new Runnable() {
      @Override public void run() {
        performFetch();
      }
    });
  }

  void performFetch() {
    try {
      final ProjectSettings projectSettings = segmentHTTPApi.fetchSettings();
      Segment.HANDLER.post(new Runnable() {
        @Override public void run() {
          // todo : does this need to be on the main thread?
          initialize(projectSettings);
        }
      });
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
  }

  private void enableIntegration(Integration integration, AbstractIntegration abstractIntegration,
      JsonMap settings) {
    try {
      abstractIntegration.initialize(context, settings);
      enabledIntegrations.put(integration, abstractIntegration);
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Could not initialize integration %s", integration.key());
    }
  }

  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityCreated(activity, savedInstanceState);
    }
  }

  void onActivityStarted(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityStarted(activity);
    }
  }

  void onActivityResumed(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityResumed(activity);
    }
  }

  void onActivityPaused(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityPaused(activity);
    }
  }

  void onActivityStopped(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityStopped(activity);
    }
  }

  void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivitySaveInstanceState(activity, outState);
    }
  }

  void onActivityDestroyed(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.onActivityDestroyed(activity);
    }
  }

  // Analytics Actions
  void identify(IdentifyPayload identify) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      if (isBundledIntegrationEnabledForPayload(identify, integration)) {
        integration.identify(identify);
      }
    }
  }

  void group(GroupPayload group) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      if (isBundledIntegrationEnabledForPayload(group, integration)) {
        integration.group(group);
      }
    }
  }

  public void track(TrackPayload track) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      if (isBundledIntegrationEnabledForPayload(track, integration)) {
        integration.track(track);
      }
    }
  }

  void alias(AliasPayload alias) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      if (isBundledIntegrationEnabledForPayload(alias, integration)) {
        integration.alias(alias);
      }
    }
  }

  void screen(ScreenPayload screen) {
    for (AbstractIntegration integration : enabledIntegrations.values()) {
      integration.screen(screen);
      if (isBundledIntegrationEnabledForPayload(screen, integration)) {
        integration.screen(screen);
      }
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

  public Map<String, Boolean> bundledIntegrations() {
    return serverIntegrations;
  }
}
