package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
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

import static com.segment.analytics.TestUtils.AliasPayloadBuilder;
import static com.segment.analytics.TestUtils.GroupPayloadBuilder;
import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Bugsnag.class)
public class BugsnagRobolectricTest extends AbstractIntegrationTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  BugsnagIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Bugsnag.class);
    integration = new BugsnagIntegration();
  }

  @Test @Override public void initialize() throws IllegalStateException {
    integration.initialize(context,
        new JsonMap().putValue("apiKey", "foo").putValue("useSSL", true), true);
    verifyStatic();
    Bugsnag.register(context, "foo");
    Bugsnag.setUseSSL(true);
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    when(activity.getLocalClassName()).thenReturn("foo");
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    Bugsnag.setContext("foo");
    verifyStatic();
    Bugsnag.onActivityCreate(activity);
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyStatic();
    Bugsnag.onActivityResume(activity);
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    Bugsnag.onActivityPause(activity);
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyStatic();
    Bugsnag.onActivityDestroy(activity);
  }

  @Test @Override public void identify() {
    Traits traits = new Traits().putUserId("foo").putEmail("bar").putName("baz");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    Bugsnag.setUser("foo", "bar", "baz");
    verifyStatic();
    Bugsnag.addToTab("User", "userId", "foo");
    verifyStatic();
    Bugsnag.addToTab("User", "email", "bar");
    verifyStatic();
    Bugsnag.addToTab("User", "name", "baz");
  }

  @Test @Override public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test @Override public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test @Override public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test @Override public void flush() {
    integration.flush();
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }
}
