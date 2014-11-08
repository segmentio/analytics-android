package com.segment.analytics;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Leanplum.class)
public class LeanplumRobolectricTest extends IntegrationRobolectricExam {
  @Rule public PowerMockRule rule = new PowerMockRule();

  LeanplumIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Leanplum.class);
    integration = new LeanplumIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context,
        new JsonMap().putValue("appId", "foo").putValue("clientKey", "bar"), true);
    verifyStatic();
    Leanplum.setAppIdForProductionMode("foo", "bar");
    verifyStatic();
    Leanplum.start(context);
  }

  @Test public void initializeWithDebugging() throws IllegalStateException {
    // Same as above, just until we verify if enabling development mode is the right behaviour
    integration.initialize(context,
        new JsonMap().putValue("appId", "foo").putValue("clientKey", "bar"), false);
    verifyStatic();
    Leanplum.setAppIdForProductionMode("foo", "bar");
    verifyStatic();
    Leanplum.start(context);
  }

  @Test public void onActivityCreated() {
    assertThat(integration.helper).isNull();
    integration.onActivityCreated(activity, bundle);
    // todo: mock helper constructor
    assertThat(integration.helper).isNotNull();
  }

  @Test public void activityLifecycle() {
    integration.helper = mock(LeanplumActivityHelper.class);

    integration.onActivityResumed(activity);
    verify(integration.helper).onResume();
    integration.onActivityPaused(activity);
    verify(integration.helper).onPause();
    integration.onActivityStopped(activity);
    verify(integration.helper).onStop();

    integration.onActivityDestroyed(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(integration.helper);
  }

  @Test public void track() {
    integration.track(trackPayload("foo"));
    verifyStatic();
    Leanplum.track("foo", 0, properties);
  }

  @Test public void screen() {
    integration.screen(screenPayload("foo", "bar"));
    verifyStatic();
    Leanplum.advanceTo("bar", "foo", properties);
  }

  @Test public void identify() {
    integration.identify(identifyPayload("foo"));
    verifyStatic();
    Leanplum.setUserAttributes("foo", traits);
  }

  @Test public void flush() {
    // exercise a bug where we were calling .forceContentUpdate in Leanplum's flush earlier
    verifyStatic();
    integration.flush();
    PowerMockito.verifyNoMoreInteractions(Leanplum.class);
  }
}
