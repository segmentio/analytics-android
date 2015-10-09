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

public class FollowAnalyticsIntegration extends AbstractIntegration<Void> {

    static final String FAID = "faid";
    static final String FOLLOW_ANALYTICS_KEY = "followanalytics";
    static final String KEY_COLLECT_LOCATION = "collectLocation";
    static final String KEY_COLLECT_LOG = "collectLog";
    static final String KEY_SESSION_TIMEOUT = "sessionTimeout";

    private static final int SESSION_TIMEOUT_DEFAULT_VALUE = 120;


    @Override
    public void initialize(Analytics analytics, ValueMap settings) throws IllegalStateException {
        String faid = settings.getString(FAID);
        if (faid == null || faid.isEmpty()) {
            throw new IllegalArgumentException(FAID + " cannot be null");
        }
        FollowApps.init(analytics.getApplication(), faid);

        boolean canCollectLocationLog = settings.getBoolean(KEY_COLLECT_LOCATION, true);
        FollowApps.setCollectLocationLogsAuthorization(canCollectLocationLog);
        boolean canCollectLog = settings.getBoolean(KEY_COLLECT_LOG, true);
        FollowApps.setCollectLogsAuthorization(canCollectLog);
        int sessionTimeout = settings.getInt(KEY_SESSION_TIMEOUT, SESSION_TIMEOUT_DEFAULT_VALUE);
        FollowApps.setMaximumBackgroundTimeWithinSession(sessionTimeout);
        Analytics.LogLevel logLevel = analytics.getLogLevel();
        FollowApps.setVerbose(logLevel == INFO || logLevel == VERBOSE);
        FollowApps.registerGcm();

    }

    @Override
    public void identify(IdentifyPayload identify) {
        super.identify(identify);
        String userId = identify.userId();
        if (userId == null || userId.isEmpty()) {
            return;
        }
        FollowApps.setCurrentIdentifier(userId);
        Map<String, String> attributes = identify.toStringMap();
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            FollowApps.setUserAttribute(key, value);
        }
    }

    @Override
    public void track(TrackPayload track) {
        super.track(track);
        if (track == null || track.event() == null) {
            return;
        }
        FollowApps.logEvent(track.event(), track.toStringMap());
    }

    @Override
    public String key() {
        return FOLLOW_ANALYTICS_KEY;
    }
}
