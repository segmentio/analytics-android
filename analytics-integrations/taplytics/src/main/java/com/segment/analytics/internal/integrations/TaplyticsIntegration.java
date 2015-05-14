package com.segment.analytics.internal.integrations;

import com.segment.analytics.Analytics;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.GroupPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import com.taplytics.sdk.Taplytics;
import org.json.JSONException;
import org.json.JSONObject;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Taplytics is a native mobile A/B testing platform that allows you to create new tests and push
 * them live without changing any code. Analytics, push notifications, and more.
 *
 * @see <a href="https://taplytics.com">Taplytics</a>
 * @see <a href="https://segment.com/docs/integrations/taplytics">Taplytics Integration</a>
 * @see <a href="https://github.com/taplytics/Taplytics-Android-SDK">Taplytics Android SDK</a>
 */
public class TaplyticsIntegration extends AbstractIntegration<Taplytics> {
  static final String TAPLYTICS_KEY = "Taplytics";

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    String apiKey = settings.getString("apiKey");
    Taplytics.startTaplytics(analytics.getApplication(), apiKey);
  }

  @Override public String key() {
    return TAPLYTICS_KEY;
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    JSONObject metaData = track.properties().toJsonObject();
    Taplytics.logEvent(track.event(), track.properties().value(), metaData);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);

    JSONObject userAttributes = identify.traits().toJsonObject();
    Traits traits = identify.traits();
    String userId = traits.userId();
    if (!isNullOrEmpty(userId)) {
      userAttributes.remove("userId");
      insert(userAttributes, "user_id", userId);
    }
    Taplytics.setUserAttributes(userAttributes);
  }

  private static void insert(JSONObject target, String key, Object value) {
    try {
      target.put(key, value);
    } catch (JSONException ignored) {
    }
  }

  @Override public void group(GroupPayload group) {
    super.group(group);
    JSONObject userAttributes = new JSONObject();
    insert(userAttributes, "groupId", group.groupId());
    insert(userAttributes, "groupTraits", group.traits().toJsonObject());
    Taplytics.setUserAttributes(userAttributes);
  }

  @Override public void reset() {
    super.reset();
    Taplytics.resetAppUser(null);
  }
}
