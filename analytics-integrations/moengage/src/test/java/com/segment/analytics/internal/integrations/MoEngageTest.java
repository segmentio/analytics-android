package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.moe.pushlibrary.MoEHelper;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.TestUtils;
import com.segment.analytics.Traits;
import com.segment.analytics.core.tests.BuildConfig;

import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(MoEngageIntegration.class)
public class MoEngageTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
    @Mock
    Application context;
    @Mock
    Analytics analytics;
    MoEngageIntegration integration;
    MoEHelper helper;
    @Before
    public void setUp() {
        initMocks(this);
        integration = new MoEngageIntegration();
        helper = integration.mHelper;
    }

    @Test public void activityCreate() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivityCreated(activity, bundle);
        verifyNoMoreMoEngageInteractions();
    }

    @Test public void activityStart() {
        Activity activity = mock(Activity.class);
        integration.onActivityStarted(activity);
        verifyNoMoreMoEngageInteractions();
    }

    @Test public void activityResume() {
        Activity activity = mock(Activity.class);
        integration.onActivityResumed(activity);
        verifyNoMoreMoEngageInteractions();
    }

    @Test public void activityPause() {
        Activity activity = mock(Activity.class);
        integration.onActivityPaused(activity);
        verifyNoMoreMoEngageInteractions();
    }

    @Test public void activityStop() {
        Activity activity = mock(Activity.class);
        integration.onActivityStopped(activity);
        verifyNoMoreMoEngageInteractions();
    }

    @Test
    public void activitySaveInstance() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivitySaveInstanceState(activity, bundle);
        verifyNoMoreMoEngageInteractions();
    }

    @Test public void track() {
        Activity activity = mock(Activity.class);
        integration.track(new TrackPayloadBuilder().event("foo").build());
        verifyNoMoreMoEngageInteractions();
    }

    @Test public void identify() {
        Traits traits = TestUtils.createTraits("foo").putAge(20);
        integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
        verifyNoMoreMoEngageInteractions();
    }

    private void verifyNoMoreMoEngageInteractions() {
        verifyNoMoreInteractions(MoEHelper.class);
        verifyNoMoreInteractions(helper);
    }
}