package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.moe.pushlibrary.MoEHelper;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.core.tests.BuildConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
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
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void activityStart() {
        Activity activity = mock(Activity.class);
        integration.onActivityStarted(activity);
        verifyStatic();
        helper.onStart(activity);
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void activityResume() {
        Activity activity = mock(Activity.class);
        integration.onActivityResumed(activity);
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void activityPause() {
        Activity activity = mock(Activity.class);
        integration.onActivityPaused(activity);
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void activityStop() {
        Activity activity = mock(Activity.class);
        integration.onActivityStopped(activity);
        verifyStatic();
        helper.onStop(activity);
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test
    public void activitySaveInstance() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivitySaveInstanceState(activity, bundle);
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void track() {
        integration.track(new TrackPayloadBuilder().event("foo").build());
        verifyStatic();
        MoEHelper.getInstance(context).trackEvent("foo");
        verifyNoMoreInteractions(MoEHelper.class);
    }
}