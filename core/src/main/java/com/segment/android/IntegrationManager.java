package com.segment.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.segment.android.internal.integrations.AmplitudeIntegration;
import com.segment.android.internal.integrations.Integration;
import com.segment.android.internal.integrations.InvalidConfigurationException;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.util.Logger;
import com.segment.android.json.JsonMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IntegrationManager {
  @SuppressWarnings("SpellCheckingInspection")
  public enum BundledProvider {
    AMPLITUDE,
    BUGSNAG,
    COUNTLY,
    CRITTERCISM,
    FLURRY,
    GOOGLE_ANALYTICS,
    LOCALYTICS,
    MIXPANEL,
    QUANTCAST,
    TAPSTREAM
  }

  private static final Map<String, BundledProvider> providers;

  static {
    // A Map of all providers that can be bundled.
    providers = new LinkedHashMap<String, BundledProvider>();
    providers.put("AMPLITUDE", BundledProvider.AMPLITUDE);
    providers.put("BUGSNAG", BundledProvider.BUGSNAG);
    providers.put("COUNTLY", BundledProvider.COUNTLY);
    providers.put("CRITTERCISM", BundledProvider.CRITTERCISM);
    providers.put("FLURRY", BundledProvider.FLURRY);
    providers.put("GOOGLE_ANALYTICS", BundledProvider.GOOGLE_ANALYTICS);
    providers.put("LOCALYTICS", BundledProvider.LOCALYTICS);
    providers.put("MIXPANEL", BundledProvider.MIXPANEL);
    providers.put("QUANTCAST", BundledProvider.QUANTCAST);
    providers.put("TAPSTREAM", BundledProvider.TAPSTREAM);
  }

  private final List<Integration> integrations = new LinkedList<Integration>();

  IntegrationManager(Context context, JsonMap projectSettings) {
    // todo: SLOWWWWWW. Or maybe not, but profile this and optimise it if needed
    for (String key : projectSettings.keySet()) {
      BundledProvider provider = providers.get(key);
      if (provider == null) continue;

      JsonMap integrationSettings = projectSettings.getJsonMap(key);
      switch (provider) {
        case AMPLITUDE:
          try {
            Class.forName("com.amplitude.api.Amplitude");
            Integration integration = new AmplitudeIntegration(context, integrationSettings);
            integrations.add(integration);
          } catch (ClassNotFoundException e) {
            Logger.w("Amplitude is not bundled in the app.");
          } catch (InvalidConfigurationException e) {
            Logger.e(e, "Could not initialize Amplitude's SDK.");
          }
          break;
        case BUGSNAG:
        case COUNTLY:
        case CRITTERCISM:
        case FLURRY:
        case GOOGLE_ANALYTICS:
        case LOCALYTICS:
        case MIXPANEL:
        case QUANTCAST:
        case TAPSTREAM:
        default:
          throw new IllegalArgumentException("provider is not available as a bundled integration");
      }
    }
  }

  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    for (Integration integration : integrations) {
      integration.onActivityCreated(activity, savedInstanceState);
    }
  }

  void onActivityStarted(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityStarted(activity);
    }
  }

  void onActivityResumed(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityResumed(activity);
    }
  }

  void onActivityPaused(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityPaused(activity);
    }
  }

  void onActivityStopped(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityStopped(activity);
    }
  }

  void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    for (Integration integration : integrations) {
      integration.onActivitySaveInstanceState(activity, outState);
    }
  }

  void onActivityDestroyed(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityDestroyed(activity);
    }
  }

  // Analytics Actions
  void identify(IdentifyPayload identify) {
    for (Integration integration : integrations) {
      integration.identify(identify);
    }
  }

  void group(GroupPayload group) {
    for (Integration integration : integrations) {
      integration.group(group);
    }
  }

  void track(TrackPayload track) {
    for (Integration integration : integrations) {
      integration.track(track);
    }
  }

  void alias(AliasPayload alias) {
    for (Integration integration : integrations) {
      integration.alias(alias);
    }
  }

  void screen(ScreenPayload screen) {
    for (Integration integration : integrations) {
      integration.screen(screen);
    }
  }
}
