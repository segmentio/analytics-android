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
public class LeanplumTest extends IntegrationExam {
  @Rule public PowerMockRule rule = new PowerMockRule();

  LeanplumIntegrationAdapter adapter;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Leanplum.class);
    adapter = new LeanplumIntegrationAdapter(true);
  }

  @Test public void initialize() throws InvalidConfigurationException {
    adapter.initialize(context,
        new JsonMap().putValue("appId", "foo").putValue("clientKey", "bar"));
    verifyStatic();
    Leanplum.setAppIdForProductionMode("foo", "bar");
    verifyStatic();
    Leanplum.start(context);
  }

  @Test public void onActivityCreated() {
    assertThat(adapter.helper).isNull();
    adapter.onActivityCreated(activity, bundle);
    // todo: mock helper constructor
    assertThat(adapter.helper).isNotNull();
  }

  @Test public void activityLifecycle() {
    adapter.helper = mock(LeanplumActivityHelper.class);

    adapter.onActivityResumed(activity);
    verify(adapter.helper).onResume();
    adapter.onActivityPaused(activity);
    verify(adapter.helper).onPause();
    adapter.onActivityStopped(activity);
    verify(adapter.helper).onStop();

    adapter.onActivityDestroyed(activity);
    adapter.onActivitySaveInstanceState(activity, bundle);
    adapter.onActivityStarted(activity);
    verifyNoMoreInteractions(adapter.helper);
  }

  @Test public void track() {
    adapter.track(trackPayload("foo"));
    verifyStatic();
    Leanplum.track("foo", 0, properties);
  }

  @Test public void screen() {
    adapter.screen(screenPayload("foo", "bar"));
    verifyStatic();
    Leanplum.advanceTo("bar", "foo", properties);
  }

  @Test public void identify() {
    adapter.identify(identifyPayload("foo"));
    verifyStatic();
    Leanplum.setUserAttributes("foo", traits);
  }

  @Test public void flush() {
    adapter.flush();
    verifyStatic();
    Leanplum.forceContentUpdate();
  }
}
