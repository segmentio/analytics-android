package com.segment.analytics.internal.integrations;

import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.Utils;
import com.segment.analytics.internal.model.payloads.GroupPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import com.taplytics.sdk.Taplytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

    static final Set<String> knownTraitNames = new HashSet<String>() {{
        add("name");
        add("email");
        add("gender");
        add("firstName");
        add("lastName");
        add("age");
    }};

    @Override
    public void initialize(Analytics analytics, ValueMap settings) throws IllegalStateException {
        String apiKey = settings.getString("appkey");
        settings.remove("appkey");
        HashMap options = new HashMap();

        for (Map.Entry<String, String> entry : settings.toStringMap().entrySet()) {
            if (entry.getValue().toLowerCase().equals("true") || entry.getValue().toLowerCase().equals("false")) {
                options.put(entry.getKey(), Boolean.valueOf(entry.getValue()));
            } else {
                options.put(entry.getKey(), entry.getValue());
            }
        }
        Taplytics.startTaplytics(analytics.getApplication(), apiKey, options);
    }

    @Override
    public String key() {
        return TAPLYTICS_KEY;
    }

    @Override
    public void identify(IdentifyPayload identify) {
        super.identify(identify);
        JSONObject userAttributes = new JSONObject();
        JSONObject customData = new JSONObject();
        JSONObject traits = identify.traits().toJsonObject();
        for (Iterator<String> jsonIterator = traits.keys(); jsonIterator.hasNext(); ) {
            String key = jsonIterator.next();
            try {
                Object value = traits.get(key);

                if (knownTraitNames.contains(key) && !Utils.isNullOrEmpty(value.toString())) {
                    userAttributes.put(key, value);
                } else if (key.equals("userId") && !Utils.isNullOrEmpty(value.toString())) {
                    userAttributes.put("user_id", value);
                } else {
                    customData.put(key, value);
                }
            } catch (JSONException e) {
                //Ignore this value if it is problematic.
            }
        }

        try {
            if (customData.length() > 0) {
                userAttributes.put("customData", customData);
            }
        } catch (JSONException e)

        {
            //We will just leave out the custom data if its problematic.
        }

        Taplytics.setUserAttributes(userAttributes);
    }

    @Override
    public void track(TrackPayload track) {
        super.track(track);
        JSONObject metaData = track.properties().toJsonObject();
        Taplytics.logEvent(track.event(), null, metaData);
    }


    @Override
    public void group(GroupPayload group) {
        super.group(group);

        JSONObject userAttributes = new JSONObject();
        JSONObject customData = group.traits().toJsonObject();

        try {
            userAttributes.put("customData", customData);
        } catch (JSONException e) {
            //We will just leave out the custom data if its problematic.
        }
        Taplytics.setUserAttributes(userAttributes);
    }

    @Override
    public void reset() {
        super.reset();
        Taplytics.resetAppUser(null);
    }

}
