package com.segment.analytics;

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
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(KahunaAnalytics.class)
public class KahunaRobolectricTest extends IntegrationRobolectricExam {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  KahunaIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(KahunaAnalytics.class);
    integration = new KahunaIntegration();
  }

  @Test
  public void initialize() throws IllegalStateException {
    integration.initialize(context,
        new JsonMap().putValue("appKey", "foo").putValue("pushSenderId", "bar"), true);

    verifyStatic();
    KahunaAnalytics.onAppCreate(context, "foo", "bar");
  }

  @Test
  public void activityLifecycle() {
    integration.onActivityStarted(activity);
    verifyStatic();
    KahunaAnalytics.start();

    integration.onActivityStopped(activity);
    verifyStatic();
    KahunaAnalytics.stop();

    integration.onActivityPaused(activity);
    integration.onActivityResumed(activity);
    integration.onActivityCreated(activity, bundle);
    integration.onActivityDestroyed(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test
  public void track() {
    integration.track(trackPayload("foo"));
    verifyStatic();
    KahunaAnalytics.trackEvent("foo", 0, 0);

    properties.putValue("count", 3);
    properties.putValue(10);
    integration.track(trackPayload("bar"));
    verifyStatic();
    KahunaAnalytics.trackEvent("bar", 3, 10);
  }

  @Test
  public void identify() {
    integration.identify(identifyPayload("foo"));
    verifyStatic();
    KahunaAnalytics.setUsernameAndEmail("foo", null);
    KahunaAnalytics.setUserCredential(USERNAME_KEY, null);
    KahunaAnalytics.setUserCredential(EMAIL_KEY, null);
    KahunaAnalytics.setUserCredential(FACEBOOK_KEY, null);
    KahunaAnalytics.setUserCredential(TWITTER_KEY, null);
    KahunaAnalytics.setUserCredential(LINKEDIN_KEY, null);
  }

  @Test
  public void identifyWithSocialAttributes() {
    traits.putUsername("foo")
        .putEmail("bar")
        .putValue(FACEBOOK_KEY, "facebook")
        .putValue(TWITTER_KEY, "twitter")
        .putValue(LINKEDIN_KEY, "linkedin");
    integration.identify(identifyPayload("baz"));
    verifyStatic();
    KahunaAnalytics.setUsernameAndEmail("foo", "bar");
    KahunaAnalytics.setUserCredential(USERNAME_KEY, "foo");
    KahunaAnalytics.setUserCredential(EMAIL_KEY, "bar");
    KahunaAnalytics.setUserCredential(FACEBOOK_KEY, "facebook");
    KahunaAnalytics.setUserCredential(TWITTER_KEY, "twitter");
    KahunaAnalytics.setUserCredential(LINKEDIN_KEY, "linkedin");
  }
}