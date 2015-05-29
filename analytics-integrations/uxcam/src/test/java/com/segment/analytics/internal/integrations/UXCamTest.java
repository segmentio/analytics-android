package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.segment.analytics.Analytics;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.createContext;
import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.segment.analytics.internal.integrations.UXCamIntegration;
import com.uxcam.UXCam;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*"})
@PrepareForTest(UXCam.class)
public class UXCamTest {

    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
    @Mock Application context;
    @Mock Analytics analytics;
    UXCamIntegration integration;

    @Before public void setUp(){
        initMocks(this);
        PowerMockito.mockStatic(UXCam.class);
        integration = new UXCamIntegration();
    }

    @Test public void initialize() throws IllegalStateException {
        when(analytics.getApplication()).thenReturn(context);
        integration.initialize(analytics, new ValueMap().putValue("accountKey", "foo"));
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void activityCreate() {
        integration.initialize(analytics, new ValueMap().putValue("accountKey", "foo"));
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivityCreated(activity, bundle);
        verifyStatic();
        UXCam.startWithKeyForSegment(activity, "foo");
    }

    @Test public void activityStart() {
        Activity activity = mock(Activity.class);
        integration.onActivityStarted(activity);
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void activityResume() {
        Activity activity = mock(Activity.class);
        integration.onActivityResumed(activity);
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void activityPause() {
        Activity activity = mock(Activity.class);
        integration.onActivityPaused(activity);
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void activityStop() {
        Activity activity = mock(Activity.class);
        integration.onActivityStopped(activity);
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void activitySaveInstance() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivitySaveInstanceState(activity, bundle);
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void activityDestroy() {
        Activity activity = mock(Activity.class);
        integration.onActivityDestroyed(activity);
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void screen() {
        integration.screen(new ScreenPayloadBuilder().name("foo").category("bar").build());
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void flush() {
        integration.flush();
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void track() {
        integration.track(new TrackPayloadBuilder().event("bar").build());
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void alias() {
        integration.alias(new AliasPayloadBuilder().build());
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void identify() {
        integration.identify(new IdentifyPayloadBuilder().traits(createTraits("foo")).build());
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void identifyWithTraits() {
        Traits traits = createTraits("bar").putAge(3).putGender("f");
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void group() {
        integration.group(new GroupPayloadBuilder().build());
        verifyNoMoreInteractions(UXCam.class);
    }

    @Test public void reset() {
        integration.reset();
        verifyNoMoreInteractions(UXCam.class);
    }
}