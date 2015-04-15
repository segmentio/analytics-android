package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.bugsnag.android.Bugsnag;
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

import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Bugsnag.class)
public class BugsnagTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  BugsnagIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Bugsnag.class);
    integration = new BugsnagIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("apiKey", "foo"), NONE);
    verifyStatic();
    Bugsnag.init(context, "foo");
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    when(activity.getLocalClassName()).thenReturn("foo");
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    Bugsnag.setContext("foo");
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void identify() {
    Traits traits = createTraits("foo").putEmail("bar").putName("baz");
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

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    Bugsnag.leaveBreadcrumb("foo");
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyStatic();
    Bugsnag.leaveBreadcrumb("Viewed foo Screen");
  }

  @Test public void flush() {
    integration.flush();
    verifyStatic();
    verifyNoMoreInteractions(Bugsnag.class);
  }
}
