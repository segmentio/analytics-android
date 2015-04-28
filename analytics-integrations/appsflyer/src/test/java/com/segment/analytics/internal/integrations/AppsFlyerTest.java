package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
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
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.BASIC;
import static com.segment.analytics.TestUtils.createTraits;
import static com.segment.analytics.internal.integrations.AppsFlyerIntegration.AppsFlyer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class AppsFlyerTest {

  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
  @Mock Application context;
  @Mock AppsFlyer appsFlyer;
  @Mock Analytics analytics;
  AppsFlyerIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    when(analytics.getApplication()).thenReturn(context);
    integration = new AppsFlyerIntegration(appsFlyer);
    integration.context = context;
  }

  @Test public void initialize() throws IllegalStateException {
    AppsFlyerIntegration integration = new AppsFlyerIntegration(appsFlyer);
    integration.initialize(analytics, new ValueMap() //
        .putValue("appsFlyerDevKey", "foo").putValue("httpFallback", true));
    verify(appsFlyer).setAppsFlyerKey("foo");
    verify(appsFlyer).setUseHTTPFallback(true);
    assertThat(integration.context).isEqualTo(context);
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void identify() {
    Traits traits = createTraits("foo").putEmail("bar");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(appsFlyer).setAppUserId("foo");
    verify(appsFlyer).setUserEmail("bar");
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void track() {
    Properties properties = new Properties().putCurrency("foo").putValue(20);
    integration.track(new TrackPayloadBuilder().properties(properties).event("baz").build());
    verify(appsFlyer).setCurrencyCode("foo");
    verify(appsFlyer).sendTrackingWithEvent(context, "baz", "20.0");
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(appsFlyer);
  }

  @Test public void reset() {
    integration.reset();
    verifyNoMoreInteractions(appsFlyer);
  }
}
