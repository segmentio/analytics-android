package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.AppsFlyerIntegration.AppsFlyer;
import static com.segment.analytics.TestUtils.AliasPayloadBuilder;
import static com.segment.analytics.TestUtils.GroupPayloadBuilder;
import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class AppsFlyerTest extends AbstractIntegrationTestCase {
  AppsFlyerIntegration integration;
  @Mock AppsFlyer appsFlyer;

  @Before public void setUp() {
    super.setUp();
    integration = new AppsFlyerIntegration(appsFlyer);
    integration.context = context;
  }

  @Test @Override public void initialize() throws IllegalStateException {
    AppsFlyerIntegration integration = new AppsFlyerIntegration(appsFlyer);
    integration.initialize(context,
        new ValueMap().putValue("appsFlyerDevKey", "foo").putValue("httpFallback", true), true);
    verify(appsFlyer).setAppsFlyerKey("foo");
    verify(appsFlyer).setUseHTTPFallback(true);
    assertThat(integration.context).isEqualTo(context);
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void identify() {
    Traits traits = new Traits().putUserId("foo").putEmail("bar");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(appsFlyer).setAppUserId("foo");
    verify(appsFlyer).setUserEmail("bar");
  }

  @Test @Override public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void track() {
    Properties properties = new Properties().putCurrency("foo").putValue(20);
    integration.track(new TrackPayloadBuilder().properties(properties).event("baz").build());
    verify(appsFlyer).setCurrencyCode("foo");
    verify(appsFlyer).sendTrackingWithEvent(context, "baz", "20.0");
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test @Override public void flush() {
    integration.flush();
    verifyNoMoreInteractions(appsFlyer);
  }
}
