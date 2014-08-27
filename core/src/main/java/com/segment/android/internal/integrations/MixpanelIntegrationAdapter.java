package com.segment.android.internal.integrations;

import android.content.Context;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.android.Properties;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;
import org.json.JSONObject;

import static com.segment.android.internal.Utils.isNullOrEmpty;

/**
 * Mixpanel is an event tracking tool targeted at web apps with lots of features: funnel, retention
 * and people tracking; advanced segmentation; and sending email and notifications.
 *
 * @see {@link https://mixpanel.com}
 * @see {@link https://segment.io/docs/integrations/mixpanel}
 * @see {@link https://github.com/mixpanel/mixpanel-android}
 */
public class MixpanelIntegrationAdapter extends AbstractIntegrationAdapter<MixpanelAPI> {
  MixpanelAPI mixpanelAPI;
  boolean isPeopleEnabled;
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;

  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    trackAllPages = settings.getBoolean("trackAllPages");
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages");
    trackNamedPages = settings.getBoolean("trackNamedPages");
    mixpanelAPI = MixpanelAPI.getInstance(context, settings.getString("token"));
    isPeopleEnabled = settings.getBoolean("people");
  }

  @Override public MixpanelAPI getUnderlyingInstance() {
    return mixpanelAPI;
  }

  @Override public String className() {
    return "com.mixpanel.android.mpmetrics.MixpanelAPI";
  }

  @Override public String key() {
    return "Mixpanel";
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

  @Override
  public void screen(ScreenPayload screen) {
    if (trackAllPages) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
  }

  @Override
  public void track(TrackPayload track) {
    event(track.event(), track.properties());
  }

  private void event(String name, Properties properties) {
    JSONObject props = properties.toJsonObject();
    mixpanelAPI.track(name, props);
    if (isPeopleEnabled) {
      Double revenue = properties.getDouble("revenue");
      if (revenue != null) {
        mixpanelAPI.getPeople().trackCharge(revenue, props);
      }
    }
  }
}
