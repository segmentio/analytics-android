package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import org.json.JSONObject;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Mixpanel is an event tracking tool targeted at web apps with lots of features: funnel, retention
 * and people tracking; advanced segmentation; and sending email and notifications.
 *
 * @see <a href="https://mixpanel.com">Mixpanel</a>
 * @see <a href="https://segment.com/docs/integrations/mixpanel">Mixpanel Integration</a>
 * @see <a href="https://github.com/mixpanel/mixpanel-android">Mixpanel Android SDK</a>
 */
public class MixpanelIntegration extends AbstractIntegration<MixpanelAPI> {
  static final String MIXPANEL_KEY = "Mixpanel";
  MixpanelAPI mixpanelAPI;
  boolean isPeopleEnabled;
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;
  String token;

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    trackAllPages = settings.getBoolean("trackAllPages", false);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", true);
    trackNamedPages = settings.getBoolean("trackNamedPages", true);
    isPeopleEnabled = settings.getBoolean("people", false);

    token = settings.getString("token");

    mixpanelAPI = MixpanelAPI.getInstance(context, token);
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);

    // This is needed to trigger a call to #checkIntentForInboundAppLink.
    // From Mixpanel's source, this won't trigger a creation of another instance. It caches
    // instances by the application context and token, both of which remain the same.
    MixpanelAPI.getInstance(activity, token);
  }

  @Override public MixpanelAPI getUnderlyingInstance() {
    return mixpanelAPI;
  }

  @Override public String key() {
    return MIXPANEL_KEY;
  }

  @Override public void identify(IdentifyPayload identify) {
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

  @Override public void flush() {
    super.flush();
    mixpanelAPI.flush();
  }

  @Override public void alias(AliasPayload alias) {
    super.alias(alias);
    mixpanelAPI.alias(alias.userId(), alias.previousId());
  }

  @Override public void screen(ScreenPayload screen) {
    if (trackAllPages) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
  }

  @Override public void track(TrackPayload track) {
    event(track.event(), track.properties());
  }

  void event(String name, Properties properties) {
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
