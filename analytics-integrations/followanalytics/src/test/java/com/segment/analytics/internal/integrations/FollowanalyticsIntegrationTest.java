package com.segment.analytics.internal.integrations;

import com.followapps.android.FollowApps;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.integrations.followanalytics.BuildConfig;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(FollowApps.class)

public class FollowAnalyticsIntegrationTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    @Rule
    public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
    @Mock
    Analytics analytics;
    FollowAnalyticsIntegration integration = new FollowAnalyticsIntegration();


    @Before
    public void setUp() {
        initMocks(this);
        PowerMockito.mockStatic(FollowApps.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initializeShouldThrowIllegalArgumentExceptionForEmptyOrNullFaid() {
        ValueMap valueMap = Mockito.mock(ValueMap.class);
        Mockito.when(valueMap.getString(FollowAnalyticsIntegration.FAID)).thenReturn(null);

        integration.initialize(analytics, valueMap);

        Assert.assertTrue("Should never be here", false);
    }


    @Test
    public void initialize() {
        ValueMap valueMap = new ValueMap().putValue(FollowAnalyticsIntegration.FAID, "faid_12EGDGH");

        integration.initialize(analytics, valueMap);

        PowerMockito.verifyStatic();
        FollowApps.setCollectLocationLogsAuthorization(true);

        PowerMockito.verifyStatic();
        FollowApps.setCollectLogsAuthorization(true);

        PowerMockito.verifyStatic();
        FollowApps.setMaximumBackgroundTimeWithinSession(120);
    }

    @Test
    public void initializeShouldSetupFollowValueMap() {
        ValueMap valueMap = new ValueMap().putValue(FollowAnalyticsIntegration.FAID, ("faid_12EGDGH"))
                .putValue(FollowAnalyticsIntegration.KEY_COLLECT_LOCATION, false)
                .putValue(FollowAnalyticsIntegration.KEY_COLLECT_LOG, true)
                .putValue(FollowAnalyticsIntegration.KEY_SESSION_TIMEOUT, 180);

        integration.initialize(analytics, valueMap);

        PowerMockito.verifyStatic();
        FollowApps.setCollectLocationLogsAuthorization(false);

        PowerMockito.verifyStatic();
        FollowApps.setCollectLogsAuthorization(true);

        PowerMockito.verifyStatic();
        FollowApps.setMaximumBackgroundTimeWithinSession(180);
    }


    @Test
    public void identify() {
        IdentifyPayload payload = Mockito.mock(IdentifyPayload.class);
        Mockito.when(payload.userId()).thenReturn("user_foo");

        integration.identify(payload);

        PowerMockito.verifyStatic();
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        FollowApps.setCurrentIdentifier(captor.capture());
        Assert.assertEquals("user_foo", captor.getValue());
    }

    @Test
    public void identifyShouldNotSetNullAsUserId() {
        IdentifyPayload payload = Mockito.mock(IdentifyPayload.class);
        Mockito.when(payload.userId()).thenReturn(null);

        integration.identify(payload);

        PowerMockito.verifyStatic();
        verifyNoMoreInteractions(FollowApps.class);

    }

    @Test
    public void identifyShouldNotSetEmptyAsUserId() {
        IdentifyPayload payload = Mockito.mock(IdentifyPayload.class);
        Mockito.when(payload.userId()).thenReturn("");

        integration.identify(payload);

        PowerMockito.verifyStatic();
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void identifyWithInvalidUserIdShouldNotSetAttributes() {
        IdentifyPayload payload = Mockito.mock(IdentifyPayload.class);
        Mockito.when(payload.userId()).thenReturn("");

        integration.identify(payload);

        PowerMockito.verifyStatic(Mockito.never());
        FollowApps.setUserAttribute(Mockito.anyString(), Mockito.anyString());
    }


    @Test
    public void identifyWithValidUserIdAndPropertyShouldSetAttributes() {
        IdentifyPayload payload = Mockito.mock(IdentifyPayload.class);
        Mockito.when(payload.userId()).thenReturn("valid_username");
        Map<String, String> maps = new HashMap<>();
        maps.put("age", "12");
        maps.put("email", "foo@bar.com");
        Mockito.when(payload.toStringMap()).thenReturn(maps);

        integration.identify(payload);


        PowerMockito.verifyStatic();
        FollowApps.setUserAttribute("age", "12");

        PowerMockito.verifyStatic();
        FollowApps.setUserAttribute("email", "foo@bar.com");
    }

    @Test
    public void identifyWithValidUserIdAndNullOrEmptyPropertyShouldNotSetAttributes() {
        IdentifyPayload payload = Mockito.mock(IdentifyPayload.class);
        Mockito.when(payload.toStringMap()).thenReturn(null);

        integration.identify(payload);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        FollowApps.setCurrentIdentifier(captor.capture());

        PowerMockito.verifyStatic(Mockito.never());
        FollowApps.setUserAttribute(Mockito.anyString(), Mockito.anyString());
    }


    @Test
    public void track() {
        TrackPayload trackPayload = Mockito.mock(TrackPayload.class);
        Mockito.when(trackPayload.event()).thenReturn("foo_event");

        HashMap<String, String> maps = new HashMap<>();
        maps.put("start_date", "12/12/2014");
        maps.put("screen", "payment");
        Mockito.when(trackPayload.toStringMap()).thenReturn(maps);

        integration.track(trackPayload);
        PowerMockito.verifyStatic(Mockito.timeout(1));

        FollowApps.logEvent(Mockito.anyString(), Mockito.anyMap());
    }

    @Test
    public void trackWithNullEventShouldNotLogEvent() {
        TrackPayload trackPayload = Mockito.mock(TrackPayload.class);
        Mockito.when(trackPayload.event()).thenReturn(null);

        integration.track(trackPayload);

        PowerMockito.verifyStatic(Mockito.never());
        FollowApps.logEvent(Mockito.anyString(), Mockito.anyMap());

        verifyNoMoreInteractions(FollowApps.class);
    }


    @Test
    public void trackWithNullPropertiesShouldLogEvent() {
        TrackPayload trackPayload = Mockito.mock(TrackPayload.class);
        Mockito.when(trackPayload.event()).thenReturn("foo_event");

        integration.track(trackPayload);
        PowerMockito.verifyStatic();

        FollowApps.logEvent("foo_event", new HashMap<String, Object>());
    }


    @Test
    public void activityCreate() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void activityStart() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void activityResume() {
        verifyNoMoreInteractions(FollowApps.class);
    }


    @Test
    public void group() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void activityPause() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void activityStop() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void activitySaveInstance() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void activityDestroy() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void screen() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void flush() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void reset() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test
    public void alias() {
        verifyNoMoreInteractions(FollowApps.class);
    }


}