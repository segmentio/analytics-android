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

  private static void transformSpecialProperties(JSONObject jsonObject, Traits traits)
      throws JSONException {
    putIfNotNull(traits.email(), jsonObject, "$email");
    putIfNotNull(traits.phone(), jsonObject, "$phone");
    putIfNotNull(traits.firstName(), jsonObject, "$first_name");
    putIfNotNull(traits.lastName(), jsonObject, "$last_name");
    putIfNotNull(traits.name(), jsonObject, "$name");
    putIfNotNull(traits.username(), jsonObject, "$username");
    putIfNotNull(traits.createdAt(), jsonObject, "$created");

    jsonObject.remove("email");
    jsonObject.remove("phone");
    jsonObject.remove("firstName");
    jsonObject.remove("lastName");
    jsonObject.remove("name");
    jsonObject.remove("username");
    jsonObject.remove("createdAt");
  }

  private static void putIfNotNull(Object value, JSONObject target, String key) {
    if (value != null) {
      try {
        target.put(key, value);
      } catch (JSONException ignored) {
      }
    }
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
    try {
      transformSpecialProperties(traits, identify.traits());
    } catch (JSONException e) {
      if (logLevel.log()) {
        debug("Could not add special properties to JSONObject for Mixpanel Integration");
      }
    }

    mixpanelAPI.registerSuperProperties(traits);
    if (isPeopleEnabled) {
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
    String previousId = alias.previousId();
    if (previousId.equals(alias.anonymousId())) {
      // If the previous ID is an anonymous ID, pass null to mixpanel, which has generated it's own
      // anonymous ID
      previousId = null;
    }
    mixpanelAPI.alias(alias.userId(), previousId);
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
    String event = track.event();

    if (increments.contains(event) && isPeopleEnabled) {
      people.increment(event, 1);
      people.set("Last " + event, new Date());
    } else {
      event(track.event(), track.properties());
    }
  }

  void event(String name, Properties properties) {
    JSONObject props = properties.toJsonObject();
    mixpanelAPI.track(name, props);
    if (isPeopleEnabled) {
      double revenue = properties.revenue();
      if (revenue != 0) {
        people.trackCharge(revenue, props);
      }
    }
  }
}
