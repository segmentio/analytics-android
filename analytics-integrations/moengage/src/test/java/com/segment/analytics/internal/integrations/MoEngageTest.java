package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.moe.pushlibrary.MoEHelper;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.TestUtils;
import com.segment.analytics.Traits;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;

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

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"  })
@PrepareForTest(MoEngageIntegration.class)
public class MoEngageTest {

    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();

    @Mock Application application;
    @Mock Context context;
    @Mock Analytics analytics;
    MoEngageIntegration integration;

    @Before
    public void setUp() {
        initMocks(this);
        integration = new MoEngageIntegration();
        integration.mHelper = MoEHelper.getInstance(application);
    }

    @Test public void initialize(){
        //nothing to be done
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
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void activityDestroy(){
        //do nothing
    }

    @Test public void group() {
        integration.group(new GroupPayloadBuilder().build());
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void alias() {
        integration.alias(new AliasPayloadBuilder().build());
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
        Activity activity = mock(Activity.class);
        integration.track(new TrackPayloadBuilder().event("foo").build());
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void screen() {
        integration.screen(new ScreenPayloadBuilder().build());
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void identify() {
        Traits traits = TestUtils.createTraits("foo");
        traits.putEmail("foo@bar.com");
        integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void flush() {
        integration.flush();
        verifyNoMoreInteractions(MoEHelper.class);
    }

    @Test public void reset() {
        integration.reset();
        verifyNoMoreInteractions(MoEHelper.class);
    }

}