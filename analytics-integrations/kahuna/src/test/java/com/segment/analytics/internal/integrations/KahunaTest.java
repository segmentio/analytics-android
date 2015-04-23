package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.kahuna.sdk.KahunaAnalytics;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.kahuna.sdk.KahunaUserCredentialKeys.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USERNAME_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USER_ID_KEY;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(KahunaAnalytics.class)
public class KahunaTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
  @Mock Application context;
  KahunaIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(KahunaAnalytics.class);
    integration = new KahunaIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context,
            new ValueMap().putValue("apiKey", "foo").putValue("pushSenderId", "bar"), NONE);

    verifyStatic();
    KahunaAnalytics.onAppCreate(context, "foo", "bar");
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    KahunaAnalytics.start();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    KahunaAnalytics.stop();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    KahunaAnalytics.trackEvent("foo");

    integration.track(new TrackPayloadBuilder().event("bar")
            .properties(new Properties().putValue("quantity", 3).putRevenue(10))
            .build());
    verifyStatic();
    KahunaAnalytics.trackEvent("bar", 3, 1000);
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().newId("myUserId").build());
    verifyStatic();
    KahunaAnalytics.setUserCredential(USER_ID_KEY, "myUserId");
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder().build());
    verifyStatic();
    KahunaAnalytics.setUserCredential(USER_ID_KEY, null);

    Traits traits = createTraits("someId");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    KahunaAnalytics.setUserCredential(USER_ID_KEY, "someId");
  }

  @Test public void identifyWithSocialAttributes() {
    Traits traits = new Traits().putUsername("foo")
            .putEmail("bar")
            .putValue(FACEBOOK_KEY, "baz")
            .putValue(TWITTER_KEY, "qux")
            .putValue(LINKEDIN_KEY, "quux");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    KahunaAnalytics.setUserCredential(USERNAME_KEY, "foo");
    verifyStatic();
    KahunaAnalytics.setUserCredential(EMAIL_KEY, "bar");
    verifyStatic();
    KahunaAnalytics.setUserCredential(FACEBOOK_KEY, "baz");
    verifyStatic();
    KahunaAnalytics.setUserCredential(TWITTER_KEY, "qux");
    verifyStatic();
    KahunaAnalytics.setUserCredential(LINKEDIN_KEY, "quux");
  }

  @Test public void reset() {
    integration.reset();
    verifyStatic();
    KahunaAnalytics.logout();
  }
}
