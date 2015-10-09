package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.os.Bundle;
import com.followapps.android.FollowApps;
import com.segment.analytics.*;
import com.segment.analytics.internal.integrations.followanalytics.BuildConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.NONE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(FollowApps.class)

public class FollowanalyticsIntegrationTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
    @Mock
    Analytics analytics;
    FollowanalyticsIntegration integration = new FollowanalyticsIntegration();


    @Before
    public void setUp() {
        initMocks(this);
        PowerMockito.mockStatic(FollowApps.class);
    }
    @After
    public void tearDown() {
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialize() throws IllegalStateException {
        when(analytics.getApplication()).thenReturn(RuntimeEnvironment.application);
        when(analytics.getLogLevel()).thenReturn(NONE);
        integration.initialize(analytics, new ValueMap());
    }

    @Test public void activityCreate() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivityCreated(activity, bundle);
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void activityStart() {
        Activity activity = mock(Activity.class);
        integration.onActivityStarted(activity);
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void activityResume() {
        Activity activity = mock(Activity.class);
        integration.onActivityResumed(activity);
        verifyNoMoreInteractions(FollowApps.class);
    }
    @Test public void identify() {
        Activity activity = mock(Activity.class);
        integration.onActivityResumed(activity);
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void group() {
        Activity activity = mock(Activity.class);
        integration.onActivityResumed(activity);
        verifyNoMoreInteractions(FollowApps.class);
    }
    @Test public void activityPause() {
        Activity activity = mock(Activity.class);
        integration.onActivityPaused(activity);
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void activityStop() {
        Activity activity = mock(Activity.class);
        integration.onActivityStopped(activity);
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void activitySaveInstance() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivitySaveInstanceState(activity, bundle);
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void activityDestroy() {
        Activity activity = mock(Activity.class);
        integration.onActivityDestroyed(activity);
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void screen() {
        integration.reset();
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void flush() {
        integration.flush();
        verifyNoMoreInteractions(FollowApps.class);
    }

    @Test public void reset() {
        integration.reset();
        verifyNoMoreInteractions(FollowApps.class);
    }
    @Test public void track() {
        integration.reset();
        verifyNoMoreInteractions(FollowApps.class);
    }
    @Test public void alias() {
        integration.reset();
        verifyNoMoreInteractions(FollowApps.class);
    }


}