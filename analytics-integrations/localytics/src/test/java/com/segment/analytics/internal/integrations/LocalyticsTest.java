package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.localytics.android.Localytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.HashMap;
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

import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.TestUtils.createTraits;
import static org.assertj.core.api.Assertions.assertThat;
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
  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
  @Mock Analytics analytics;
  LocalyticsIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Localytics.class);

    integration = new LocalyticsIntegration();
    integration.customDimensions = new ValueMap();
    integration.hasSupportLibOnClassPath = true;
  }

  @After public void tearDown() {
    verifyNoMoreInteractions(Localytics.class);
  }

  @Test public void initialize() throws IllegalStateException {
    when(analytics.getApplication()).thenReturn(RuntimeEnvironment.application);
    when(analytics.getLogLevel()).thenReturn(NONE);
    LocalyticsIntegration integration = new LocalyticsIntegration();

    ValueMap customDimensions = new ValueMap().putValue("bar", "baz").putValue("qaz", 0);
    integration.initialize(analytics, new ValueMap() //
        .putValue("appKey", "foo").putValue("dimensions", customDimensions));

    verifyStatic();
    Localytics.integrate(RuntimeEnvironment.application, "foo");
    verifyStatic();
    Localytics.setLoggingEnabled(false);
    assertThat(integration.customDimensions).isEqualTo(customDimensions);
  }

  @Test public void initializeWithVerbose() throws IllegalStateException {
    when(analytics.getApplication()).thenReturn(RuntimeEnvironment.application);
    when(analytics.getLogLevel()).thenReturn(VERBOSE);
    LocalyticsIntegration integration = new LocalyticsIntegration();
    PowerMockito.mockStatic(Localytics.class);

    integration.initialize(analytics, new ValueMap().putValue("appKey", "foo"));

    verifyStatic();
    Localytics.integrate(RuntimeEnvironment.application, "foo");
    verifyStatic();
    Localytics.setLoggingEnabled(true);
  }

  @Test public void initializeWithInfo() throws IllegalStateException {
    when(analytics.getApplication()).thenReturn(RuntimeEnvironment.application);
    when(analytics.getLogLevel()).thenReturn(INFO);
    LocalyticsIntegration integration = new LocalyticsIntegration();
    PowerMockito.mockStatic(Localytics.class);

    integration.initialize(analytics, new ValueMap().putValue("appKey", "foo"));

    verifyStatic();
    Localytics.integrate(RuntimeEnvironment.application, "foo");
    verifyStatic();
    Localytics.setLoggingEnabled(true);
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
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
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    Localytics.closeSession();
    verifyStatic();
    Localytics.upload();
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
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder().traits(createTraits("foo")).build());

    verifyStatic();
    Localytics.setCustomerId("foo");
    verifyStatic();
    Localytics.setProfileAttribute("userId", "foo", Localytics.ProfileScope.APPLICATION);
  }

  @Test public void identifyWithSpecialFields() {
    integration.identify(new IdentifyPayloadBuilder().traits(
        createTraits("foo").putEmail("baz").putName("bar").putValue("custom", "qaz")).build());

    verifyStatic();
    Localytics.setCustomerId("foo");
    verifyStatic();
    Localytics.setIdentifier("email", "baz");
    verifyStatic();
    Localytics.setIdentifier("customer_name", "bar");
    verifyStatic();
    Localytics.setProfileAttribute("userId", "foo", Localytics.ProfileScope.APPLICATION);
    verifyStatic();
    Localytics.setProfileAttribute("email", "baz", Localytics.ProfileScope.APPLICATION);
    verifyStatic();
    Localytics.setProfileAttribute("name", "bar", Localytics.ProfileScope.APPLICATION);
    verifyStatic();
    Localytics.setProfileAttribute("custom", "qaz", Localytics.ProfileScope.APPLICATION);
  }

  @Test public void identifyWithCustomDimensions() {
    integration.customDimensions = new ValueMap().putValue("foo", 1);

    integration.identify(new IdentifyPayloadBuilder() //
        .traits(createTraits("bar").putValue("foo", "baz")).build());

    verifyStatic();
    Localytics.setCustomerId("bar");
    verifyStatic();
    Localytics.setCustomDimension(1, "baz");
    verifyStatic();
    Localytics.setProfileAttribute("userId", "bar", Localytics.ProfileScope.APPLICATION);
    verifyStatic();
    Localytics.setProfileAttribute("foo", "baz", Localytics.ProfileScope.APPLICATION);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
  }

  @Test public void flush() {
    integration.flush();
    verifyStatic();
    Localytics.upload();
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

  @Test public void trackWithCustomDimensions() {
    integration.customDimensions = new ValueMap().putValue("foo", 9);

    Properties props = new Properties().putValue("foo", 1);
    integration.track(new TrackPayloadBuilder().event("bar").properties(props).build());

    verifyStatic();
    Localytics.tagEvent("bar", props.toStringMap());
    verifyStatic();
    Localytics.setCustomDimension(9, "1");
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
  }

  @Test public void reset() {
    integration.reset();
  }
}

