package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.os.Bundle;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.Log;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.toJsonObject;
import static com.segment.analytics.internal.Utils.transform;

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
  MixpanelAPI mixpanel;
  MixpanelAPI.People mixpanelPeople;
  boolean isPeopleEnabled;
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;
  String token;
  Log log;
  Set<String> increments;
  private static final Map<String, String> MAPPER;

  static {
    Map<String, String> mapper = new LinkedHashMap<>();
    mapper.put("email", "$email");
    mapper.put("phone", "$phone");
    mapper.put("first_name", "$first_name");
    mapper.put("lastName", "$last_name");
    mapper.put("$name", "$name");
    mapper.put("username", "$username");
    mapper.put("createdAt", "$created");
    MAPPER = Collections.unmodifiableMap(mapper);
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

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    log = analytics.getLogger().newLogger(MIXPANEL_KEY);

    trackAllPages = settings.getBoolean("trackAllPages", false);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", true);
    trackNamedPages = settings.getBoolean("trackNamedPages", true);
    isPeopleEnabled = settings.getBoolean("people", false);
    token = settings.getString("token");
    increments = getStringSet(settings, "increments");

    mixpanel = MixpanelAPI.getInstance(analytics.getApplication(), token);
    log.verbose("MixpanelAPI.getInstance(context, %s);", token);
    if (isPeopleEnabled) {
      mixpanelPeople = mixpanel.getPeople();
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
    return mixpanel;
  }

  @Override public String key() {
    return MIXPANEL_KEY;
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    mixpanel.identify(userId);
    log.verbose("mixpanel.identify(%s)", userId);
    JSONObject traits = toJsonObject(transform(identify.traits(), MAPPER));
    mixpanel.registerSuperProperties(traits);
    log.verbose("mixpanel.registerSuperProperties(%s)", traits);

    if (!isPeopleEnabled) {
      return;
    }
    mixpanelPeople.identify(userId);
    log.verbose("mixpanelPeople.identify(%s)", userId);
    mixpanelPeople.set(traits);
    log.verbose("mixpanelPeople.set(%s)", traits);
  }

  @Override public void flush() {
    super.flush();
    mixpanel.flush();
    log.verbose("mixpanel.flush()");
  }

  @Override public void reset() {
    super.reset();
    mixpanel.reset();
    log.verbose("mixpanel.reset()");
  }

  @Override public void alias(AliasPayload alias) {
    super.alias(alias);
    String previousId = alias.previousId();
    if (previousId.equals(alias.anonymousId())) {
      // If the previous ID is an anonymous ID, pass null to mixpanel, which has generated it's own
      // anonymous ID
      previousId = null;
    }
    mixpanel.alias(alias.userId(), previousId);
    log.verbose("mixpanel.alias(%s, %s)", alias.userId(), previousId);
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

    event(event, track.properties());

    if (increments.contains(event) && isPeopleEnabled) {
      mixpanelPeople.increment(event, 1);
      mixpanelPeople.set("Last " + event, new Date());
    }
  }

  void event(String name, Properties properties) {
    JSONObject props = properties.toJsonObject();
    mixpanel.track(name, props);
    log.verbose("mixpanel.track(%s, %s)", name, props);
    if (!isPeopleEnabled) {
      return;
    }
    double revenue = properties.revenue();
    if (revenue == 0) {
      return;
    }
    mixpanelPeople.trackCharge(revenue, props);
    log.verbose("mixpanelPeople.trackCharge(%s, %s)", revenue, props);
  }
}
