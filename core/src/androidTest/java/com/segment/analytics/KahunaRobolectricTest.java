package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import com.kahuna.sdk.KahunaAnalytics;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.kahuna.sdk.KahunaUserCredentialKeys.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USERNAME_KEY;
import static com.segment.analytics.TestUtils.AliasPayloadBuilder;
import static com.segment.analytics.TestUtils.GroupPayloadBuilder;
import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(KahunaAnalytics.class)
public class KahunaRobolectricTest extends AbstractIntegrationTest {
  @Rule public PowerMockRule rule = new PowerMockRule();

  KahunaIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(KahunaAnalytics.class);
    integration = new KahunaIntegration();
  }

  @Test @Override public void initialize() throws IllegalStateException {
    integration.initialize(context,
        new JsonMap().putValue("secretKey", "foo").putValue("pushSenderId", "bar"), true);

    verifyStatic();
    KahunaAnalytics.onAppCreate(context, "foo", "bar");
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    KahunaAnalytics.start();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    KahunaAnalytics.stop();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    KahunaAnalytics.trackEvent("foo", 0, 0);

    integration.track(new TrackPayloadBuilder().event("bar")
        .properties(new Properties().putValue("count", 3).putValue(10))
        .build());
    verifyStatic();
    KahunaAnalytics.trackEvent("bar", 3, 10);
  }

  @Test @Override public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void flush() {
    integration.flush();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test @Override public void identify() {
    integration.identify(new IdentifyPayloadBuilder().userId("foo").build());
    verifyStatic();
    KahunaAnalytics.setUsernameAndEmail("foo", null);
    KahunaAnalytics.setUserCredential(USERNAME_KEY, null);
    KahunaAnalytics.setUserCredential(EMAIL_KEY, null);
    KahunaAnalytics.setUserCredential(FACEBOOK_KEY, null);
    KahunaAnalytics.setUserCredential(TWITTER_KEY, null);
    KahunaAnalytics.setUserCredential(LINKEDIN_KEY, null);
  }

  @Test public void identifyWithSocialAttributes() {
    Traits traits = new Traits().putUsername("foo")
        .putEmail("bar")
        .putValue("facebook", "baz")
        .putValue("twitter", "qux")
        .putValue("linkedin", "quux");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    KahunaAnalytics.setUsernameAndEmail("foo", "bar");
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
}
