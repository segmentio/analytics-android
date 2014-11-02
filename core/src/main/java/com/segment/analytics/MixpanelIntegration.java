package com.segment.analytics;

import android.content.Context;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import org.json.JSONObject;

import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * Mixpanel is an event tracking tool targeted at web apps with lots of features: funnel, retention
 * and people tracking; advanced segmentation; and sending email and notifications.
 *
 * @see <a href="https://mixpanel.com">Mixpanel</a>
 * @see <a href="https://segment.io/docs/integrations/mixpanel">Mixpanel Integration</a>
 * @see <a href="https://github.com/mixpanel/mixpanel-android">Mixpanel Android SDK</a>
 */
class MixpanelIntegration extends AbstractIntegration<MixpanelAPI> {
  MixpanelAPI mixpanelAPI;
  boolean isPeopleEnabled;
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;

  MixpanelIntegration(boolean debuggingEnabled) {
    super(debuggingEnabled);
  }

  @Override void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    trackAllPages = settings.getBoolean("trackAllPages", false);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", true);
    trackNamedPages = settings.getBoolean("trackNamedPages", true);
    isPeopleEnabled = settings.getBoolean("people", false);

    mixpanelAPI = MixpanelAPI.getInstance(context, settings.getString("token"));
  }

  @Override MixpanelAPI getUnderlyingInstance() {
    return mixpanelAPI;
  }

  @Override String key() {
    return "Mixpanel";
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    mixpanelAPI.identify(userId);
    JSONObject traits = identify.traits().toJsonObject();
    mixpanelAPI.registerSuperProperties(traits);
    if (isPeopleEnabled) {
      MixpanelAPI.People people = mixpanelAPI.getPeople();
      people.identify(userId);
      people.set(traits);
    }
  }

  @Override void flush() {
    super.flush();
    mixpanelAPI.flush();
  }

  @Override void alias(AliasPayload alias) {
    super.alias(alias);
    mixpanelAPI.alias(alias.userId(), alias.previousId());
  }

  @Override void screen(ScreenPayload screen) {
    if (trackAllPages) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
  }

  @Override void track(TrackPayload track) {
    event(track.event(), track.properties());
  }

  private void event(String name, Properties properties) {
    JSONObject props = properties.toJsonObject();
    mixpanelAPI.track(name, props);
    if (isPeopleEnabled) {
      double revenue = properties.revenue();
      if (revenue != 0) {
        mixpanelAPI.getPeople().trackCharge(revenue, props);
      }
    }
  }
}
