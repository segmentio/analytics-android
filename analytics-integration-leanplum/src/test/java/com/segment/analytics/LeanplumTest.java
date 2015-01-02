package com.segment.analytics;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import java.util.HashMap;
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
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Leanplum.class)
public class LeanplumTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  LeanplumIntegration integration;
  LeanplumActivityHelper leanplumActivityHelper;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Leanplum.class);
    integration = new LeanplumIntegration();
    integration.helper = leanplumActivityHelper = mock(LeanplumActivityHelper.class);
  }

  @Test public void initialize() {
    integration.initialize(context,
        new ValueMap().putValue("appId", "foo").putValue("clientKey", "bar"), true);
    verifyStatic();
    Leanplum.setAppIdForProductionMode("foo", "bar");
    verifyStatic();
    Leanplum.start(context);
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration = new LeanplumIntegration();
    assertThat(integration.helper).isNull();
    integration.onActivityCreated(activity, bundle);
    // todo: mock helper constructor
    assertThat(integration.helper).isNotNull();
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(leanplumActivityHelper);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verify(leanplumActivityHelper).onResume();
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verify(leanplumActivityHelper).onPause();
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(leanplumActivityHelper).onStop();
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(leanplumActivityHelper);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(leanplumActivityHelper);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    Leanplum.track("foo", 0, new HashMap<String, Object>());
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Leanplum.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("foo").name("bar").build());
    verifyStatic();
    Leanplum.advanceTo("bar", "foo", new HashMap<String, Object>());
  }

  @Test public void identify() {
    Traits traits = new Traits().putUserId("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    Leanplum.setUserAttributes("foo", traits);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Leanplum.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyStatic();
    verifyNoMoreInteractions(Leanplum.class);
  }
}
