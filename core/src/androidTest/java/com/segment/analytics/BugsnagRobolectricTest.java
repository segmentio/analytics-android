package com.segment.analytics;

import com.bugsnag.android.Bugsnag;
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

import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Bugsnag.class)
public class BugsnagRobolectricTest extends IntegrationRobolectricExam {
  @Rule public PowerMockRule rule = new PowerMockRule();
  BugsnagIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Bugsnag.class);
    integration = new BugsnagIntegration(true);
  }

  @Test public void initialize() throws InvalidConfigurationException {
    integration.initialize(context, new JsonMap().putValue("apiKey", "foo").putValue("useSSL", true));
    verifyStatic();
    Bugsnag.register(context, "foo");
    Bugsnag.setUseSSL(true);
  }

  @Test
  public void onActivityCreated() {
    when(activity.getLocalClassName()).thenReturn("foo");
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    Bugsnag.setContext("foo");
    verifyStatic();
    Bugsnag.onActivityCreate(activity);
  }

  @Test public void onActivityResumed() {
    integration.onActivityResumed(activity);
    verifyStatic();
    Bugsnag.onActivityResume(activity);
  }

  @Test public void onActivityPaused() {
    integration.onActivityPaused(activity);
    verifyStatic();
    Bugsnag.onActivityPause(activity);
  }

  @Test public void onActivityDestroyed() {
    integration.onActivityDestroyed(activity);
    verifyStatic();
    Bugsnag.onActivityDestroy(activity);
  }

  @Test public void activityLifecycle() {
    integration.onActivityStopped(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    integration.onActivityStarted(activity);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void identify() {
    traits.putUserId("foo").putEmail("bar").putName("baz");
    integration.identify(identifyPayload("foo"));
    verifyStatic();
    Bugsnag.setUser("foo", "bar", "baz");
    verifyStatic();
    Bugsnag.addToTab("User", "userId", "foo");
    verifyStatic();
    Bugsnag.addToTab("User", "email", "bar");
    verifyStatic();
    Bugsnag.addToTab("User", "name", "baz");
  }
}
