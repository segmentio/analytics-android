package com.segment.android.internal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.segment.android.internal.Utils.defaultSingleThreadedExecutor;

/**
 * Manages bundled integrations. This class will maintain it's own queue for events to account for
 * the latency between receiving the first event, fetching remote settings and enabling the
 * integrations. Once we enable all integrations - we'll replay any events on disk. This should
 * only
 * affect the first app install, subsequent launches will be use a cached value from disk. Note
 * that
 * none of the activity lifecycle events are queued to disk.
 */
public class IntegrationManager {
  // A set of integrations available on the device
  private Map<String, Boolean> bundledIntegrations = new LinkedHashMap<String, Boolean>();
  private List<AbstractIntegration> availableBundledIntegrations =
      new LinkedList<AbstractIntegration>();
  // A set of integrations that are available and have been enabled for this project.
  private final List<AbstractIntegration> enabledIntegrations =
      new LinkedList<AbstractIntegration>();

  final Context context;
  final SegmentHTTPApi segmentHTTPApi;
  final Handler mainThreadHandler;
  final ExecutorService service;

  public static IntegrationManager create(Context context, Handler mainThreadHandler,
      SegmentHTTPApi segmentHTTPApi) {
    ExecutorService service = defaultSingleThreadedExecutor();
    return new IntegrationManager(context, mainThreadHandler, segmentHTTPApi, service);
  }

  IntegrationManager(Context context, Handler mainThreadHandler, SegmentHTTPApi segmentHTTPApi,
      ExecutorService service) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.mainThreadHandler = mainThreadHandler;
    this.service = service;

    service.submit(new Runnable() {
      @Override public void run() {
        init();
        performFetch();
      }
    });
  }

  void init() {
    // Look up all the integrations available on the device. This is done early so that we can
    // disable sending to these integrations from the server and properly fill the payloads.
    try {
      AbstractIntegration integration = new AmplitudeIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Amplitude not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Amplitude needs more data!");
    }
    try {
      AbstractIntegration integration = new BugsnagIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Bugsnag not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Bugsnag needs more data!");
    }
    try {
      AbstractIntegration integration = new CountlyIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Countly not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Countly needs more data!");
    }
    try {
      AbstractIntegration integration = new CrittercismIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Crittercism not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Crittercism needs more data!");
    }
    try {
      AbstractIntegration integration = new FlurryIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Flurry not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Flurry needs more data!");
    }
    try {
      AbstractIntegration integration = new GoogleAnalyticsIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Google Analytics not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Google Analytics needs more data!");
    }
    try {
      AbstractIntegration integration = new LocalyticsIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Localytics not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Localytics needs more data!");
    }
    try {
      AbstractIntegration integration = new MixpanelIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Mixpanel not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Mixpanel needs more data!");
    }
    try {
      AbstractIntegration integration = new QuantcastIntegration();
      add(integration);
    } catch (ClassNotFoundException e) {
      Logger.d("Quantcast not bundled");
    } catch (InvalidConfigurationException e) {
      Logger.e(e, "Quantcast needs more data!");
    }
  }

  void add(AbstractIntegration integration) throws InvalidConfigurationException {
    integration.validate(context);
    availableBundledIntegrations.add(integration);
    bundledIntegrations.put(integration.key(), false);
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
    for (Iterator<AbstractIntegration> it = availableBundledIntegrations.iterator();
        it.hasNext(); ) {
      AbstractIntegration integration = it.next();
      try {
        boolean enabled = integration.initialize(context, projectSettings);
        if (enabled) {
          enabledIntegrations.add(integration);
        }
      } catch (InvalidConfigurationException e) {
        Logger.e(e, "Could not load %s", integration.key());
      } finally {
        it.remove();
      }
    }
    availableBundledIntegrations = null; // Help the GC
  }

  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    for (AbstractIntegration integration : enabledIntegrations) {
      integration.onActivityCreated(activity, savedInstanceState);
    }
  }

  void onActivityStarted(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations) {
      integration.onActivityStarted(activity);
    }
  }

  void onActivityResumed(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations) {
      integration.onActivityResumed(activity);
    }
  }

  void onActivityPaused(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations) {
      integration.onActivityPaused(activity);
    }
  }

  void onActivityStopped(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations) {
      integration.onActivityStopped(activity);
    }
  }

  void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    for (AbstractIntegration integration : enabledIntegrations) {
      integration.onActivitySaveInstanceState(activity, outState);
    }
  }

  void onActivityDestroyed(Activity activity) {
    for (AbstractIntegration integration : enabledIntegrations) {
      integration.onActivityDestroyed(activity);
    }
  }

  // Analytics Actions
  void identify(IdentifyPayload identify) {
    for (AbstractIntegration integration : enabledIntegrations) {
      if (isBundledIntegrationEnabledForPayload(identify, integration)) {
        integration.identify(identify);
      }
    }
  }

  void group(GroupPayload group) {
    for (AbstractIntegration integration : enabledIntegrations) {
      if (isBundledIntegrationEnabledForPayload(group, integration)) {
        integration.group(group);
      }
    }
  }

  public void track(TrackPayload track) {
    for (AbstractIntegration integration : enabledIntegrations) {
      if (isBundledIntegrationEnabledForPayload(track, integration)) {
        integration.track(track);
      }
    }
  }

  void alias(AliasPayload alias) {
    for (AbstractIntegration integration : enabledIntegrations) {
      if (isBundledIntegrationEnabledForPayload(alias, integration)) {
        integration.alias(alias);
      }
    }
  }

  void screen(ScreenPayload screen) {
    for (AbstractIntegration integration : enabledIntegrations) {
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
    // are set to false
    JsonMap integrations = payload.context().getIntegrations();
    if (!JsonMap.isNullOrEmpty(integrations)) {
      if (integrations.containsKey(integration.key())) {
        enabled = integrations.getBoolean(integration.key());
      } else if (integrations.containsKey("All")) {
        enabled = integrations.getBoolean("All");
      } else if (integrations.containsKey("all")) {
        enabled = integrations.getBoolean("all");
      }
    }
    // need this check since users could accidentially put in their own custom values, in which
    // case the get methods will return null. Ugh mutability
    return enabled == null ? true : enabled;
  }

  public Map<String, Boolean> bundledIntegrations() {
    return Collections.unmodifiableMap(bundledIntegrations);
  }
}
