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

import java.util.Map;

class TaplyticsIntegration extends AbstractIntegration<Taplytics> {
    static final String TAPLYTICS_KEY = "Taplytics";

    @Override
    public void initialize(Analytics analytics, ValueMap settings) throws IllegalStateException {
        Taplytics.startTaplytics(analytics.getApplication(), settings.getString("apiKey"));
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
        for (Map.Entry<String, Object> entry : identify.traits().entrySet()) {
            try {
                if ((entry.getKey().equals("name") || entry.getKey().equals("email")) && Utils.isNullOrEmpty(entry.getValue().toString())) {
                    userAttributes.put(entry.getKey(), entry.getValue());
                } else {
                    customData.put(entry.getKey(), entry.getValue());
                }
            } catch (JSONException e) {
                //Ignore this value if it is problematic.
            }
        }
        try {
            userAttributes.put("customData", customData);
        } catch (JSONException e) {
            //We will just leave out the custom data if its problematic.
        }
        Taplytics.setUserAttributes(userAttributes);
    }

    @Override
    public void track(TrackPayload track) {
        super.track(track);
        JSONObject metaData = new JSONObject();
        for (Map.Entry<String, Object> entry : track.properties().entrySet()) {
            try {
                metaData.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                //Ignore this value if it is problematic.
            }
        }
        Taplytics.logEvent(track.event(), null, metaData);
    }


    @Override
    public void group(GroupPayload group) {
        super.group(group);

        JSONObject userAttributes = new JSONObject();
        JSONObject customData = new JSONObject();
        for (Map.Entry<String, Object> entry : group.traits().entrySet()) {
            try {
                customData.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                //Ignore this value if it is problematic.
            }
        }
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