package com.segment.analytics.internal.integrations;


import com.followapps.android.FollowApps;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import java.util.Map;

import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;

public class FollowanalyticsIntegration extends AbstractIntegration<Void> {


    static final String FOLLOW_ANALYTICS_KEY = "followanalytics";
    private final int BG_SESSION_TIMEOUT = 120;

    @Override
    public void initialize(Analytics analytics, ValueMap settings) throws IllegalStateException {
        String faid = settings.getString("faid");
        if(faid ==null || faid.isEmpty()){
            throw  new IllegalArgumentException("faid cannot be null");
        }
        FollowApps.init(analytics.getApplication(), faid);
        Analytics.LogLevel logLevel = analytics.getLogLevel();
        FollowApps.setVerbose(logLevel == INFO || logLevel == VERBOSE);
        FollowApps.registerGcm();
        FollowApps.setCollectLocationLogsAuthorization(settings.getBoolean("collectLocation", true));
        FollowApps.setCollectLogsAuthorization(settings.getBoolean("collectLog", true));
        FollowApps.setMaximumBackgroundTimeWithinSession(settings.getInt("sessionTimeout", BG_SESSION_TIMEOUT));
    }

    @Override
    public void identify(IdentifyPayload identify) {
        super.identify(identify);
        FollowApps.setCurrentIdentifier(identify.userId());
        for (Map.Entry<String, Object> entry : identify.traits().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            FollowApps.setUserAttribute(key, value);
        }
    }

    @Override
    public void track(TrackPayload track) {
        super.track(track);
        FollowApps.logEvent(track.event(),track.properties().toStringMap());
    }

    @Override
    public String key() {
        return FOLLOW_ANALYTICS_KEY;
    }
}
