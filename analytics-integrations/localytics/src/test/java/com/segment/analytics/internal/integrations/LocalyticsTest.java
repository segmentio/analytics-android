package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.localytics.android.Localytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(Localytics.class)
public class LocalyticsTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  LocalyticsIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Localytics.class);

    integration = new LocalyticsIntegration();
    integration.hasSupportLibOnClassPath = true;
  }

  @Test public void initialize() throws IllegalStateException {
    LocalyticsIntegration integration = new LocalyticsIntegration();

    PowerMockito.mockStatic(Localytics.class);
    integration.initialize(RuntimeEnvironment.application, new ValueMap().putValue("appKey", "foo"),
        INFO);
    verifyStatic();
    Localytics.integrate(RuntimeEnvironment.application, "foo");
    verifyStatic();
    Localytics.setLoggingEnabled(true);

    PowerMockito.mockStatic(Localytics.class);
    integration.initialize(RuntimeEnvironment.application, new ValueMap().putValue("appKey", "foo"),
        VERBOSE);
    verifyStatic();
    Localytics.integrate(RuntimeEnvironment.application, "foo");
    verifyStatic();
    Localytics.setLoggingEnabled(true);

    PowerMockito.mockStatic(Localytics.class);
    integration.initialize(RuntimeEnvironment.application, new ValueMap().putValue("appKey", "foo"),
        NONE);
    verifyStatic();
    Localytics.integrate(RuntimeEnvironment.application, "foo");
    verifyStatic();
    Localytics.setLoggingEnabled(false);
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    Intent intent = mock(Intent.class);
    when(activity.getIntent()).thenReturn(intent);
    integration.onActivityResumed(activity);
    verifyStatic();
    Localytics.openSession();
    verifyStatic();
    Localytics.upload();
    verifyStatic();
    Localytics.handleTestMode(intent);
    verifyStatic();
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void activityResumeCompat() {
    FragmentActivity activity = mock(FragmentActivity.class);
    integration.onActivityResumed(activity);
    verifyStatic();
    Localytics.openSession();
    verifyStatic();
    Localytics.upload();
    verifyStatic();
    Localytics.setInAppMessageDisplayActivity(activity);
    verifyStatic();
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    Localytics.closeSession();
    verifyStatic();
    Localytics.upload();
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void activityPauseCompat() {
    FragmentActivity activity = mock(FragmentActivity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    Localytics.dismissCurrentInAppMessage();
    verifyStatic();
    Localytics.clearInAppMessageDisplayActivity();
    verifyStatic();
    Localytics.closeSession();
    verifyStatic();
    Localytics.upload();
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder().build());
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyStatic();
    Localytics.upload();
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("foo").name("bar").build());
    verifyStatic();
    Localytics.tagScreen("bar");

    integration.screen(new ScreenPayloadBuilder().name("baz").build());
    verifyStatic();
    Localytics.tagScreen("baz");

    integration.screen(new ScreenPayloadBuilder().category("qux").build());
    verifyStatic();
    Localytics.tagScreen("qux");
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    Localytics.tagEvent("foo", new HashMap<String, String>());
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(Localytics.class);
  }
  
  @Test public void reset() {
    integration.reset();
    verifyNoMoreInteractions(Localytics.class);
  }
}

