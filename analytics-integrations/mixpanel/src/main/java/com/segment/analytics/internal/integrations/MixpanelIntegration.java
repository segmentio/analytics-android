package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.internal.Utils.debug;
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
  MixpanelAPI.People people;
  boolean isPeopleEnabled;
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;
  String token;
  LogLevel logLevel;
  Set<String> increments;

  private static void registerSuperProperties(JSONObject jsonObject, Traits traits)
      throws JSONException {
    jsonObject.put("$email", traits.email());
    jsonObject.remove("email");
    jsonObject.put("$phone", traits.phone());
    jsonObject.remove("phone");
    jsonObject.put("$first_name", traits.firstName());
    jsonObject.remove("firstName");
    jsonObject.put("$last_name", traits.lastName());
    jsonObject.remove("lastName");
    jsonObject.put("$name", traits.name());
    jsonObject.remove("name");
    jsonObject.put("$username", traits.username());
    jsonObject.remove("username");
    jsonObject.put("$created", traits.createdAt());
    jsonObject.remove("createdAt");
  }

  static Set<String> getStringSet(ValueMap valueMap, Object key) {
    try {
      List<Object> incrementEvents = (List<Object>) valueMap.get(key);
      if (isNullOrEmpty(incrementEvents)) {
        return Collections.emptySet();
      }
      Set<String> stringSet = new HashSet<>(incrementEvents.size());
      for (int i = 0; i < incrementEvents.size(); i++) {
        stringSet.add((String) incrementEvents.get(i));
      }
      return stringSet;
    } catch (ClassCastException e) {
      return Collections.emptySet();
    }
  }

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    this.logLevel = logLevel;

    trackAllPages = settings.getBoolean("trackAllPages", false);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", true);
    trackNamedPages = settings.getBoolean("trackNamedPages", true);
    isPeopleEnabled = settings.getBoolean("people", false);
    token = settings.getString("token");
    increments = getStringSet(settings, "increments");

    mixpanelAPI = MixpanelAPI.getInstance(context, token);
    if (isPeopleEnabled) {
      people = mixpanelAPI.getPeople();
    }
  }

  @Override public boolean onActivityCreated(Activity activity, Bundle savedInstanceState) {
    // This is needed to trigger a call to #checkIntentForInboundAppLink.
    // From Mixpanel's source, this won't trigger a creation of another instance. It caches
    // instances by the application context and token, both of which remain the same.
    MixpanelAPI.getInstance(activity, token);

    return true;
  }

  @Override public MixpanelAPI getUnderlyingInstance() {
    return mixpanelAPI;
  }

  @Override public String key() {
    return MIXPANEL_KEY;
  }

  @Override public boolean identify(IdentifyPayload identify) {
    String userId = identify.userId();
    mixpanelAPI.identify(userId);
    JSONObject traits = identify.traits().toJsonObject();
    try {
      registerSuperProperties(traits, identify.traits());
    } catch (JSONException e) {
      if (logLevel.log()) {
        debug("Could not add super properties to JSONObject for Mixpanel Integration");
      }
    }

    mixpanelAPI.registerSuperProperties(traits);
    if (isPeopleEnabled) {
      people.identify(userId);
      people.set(traits);
    }
    return true;
  }

  @Override public boolean flush() {
    mixpanelAPI.flush();
    return true;
  }

  @Override public boolean alias(AliasPayload alias) {
    String previousId = alias.previousId();
    if (previousId.equals(alias.anonymousId())) {
      // If the previous ID is an anonymous ID, pass null to mixpanel, which has generated it's own
      // anonymous ID
      previousId = null;
    }
    mixpanelAPI.alias(alias.userId(), previousId);
    return true;
  }

  @Override public boolean screen(ScreenPayload screen) {
    if (trackAllPages) {
      return event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      return event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      return event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
    return false;
  }

  @Override public boolean track(TrackPayload track) {
    String event = track.event();

    if (increments.contains(event) && isPeopleEnabled) {
      people.increment(event, 1);
      people.set("Last " + event, new Date());
    } else {
      event(track.event(), track.properties());
    }
    return true;
  }

  boolean event(String name, Properties properties) {
    JSONObject props = properties.toJsonObject();
    mixpanelAPI.track(name, props);
    if (isPeopleEnabled) {
      double revenue = properties.revenue();
      if (revenue != 0) {
        people.trackCharge(revenue, props);
      }
    }
    return true;
  }
}
