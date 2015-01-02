package com.segment.analytics;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.JSONObjectMatcher.jsonEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(Crittercism.class)
public class CrittercismTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @MockitoAnnotations.Mock Application context;
  CrittercismIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Crittercism.class);
    integration = new CrittercismIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("appId", "foo"), true);
    verifyStatic();
    // todo: verify config params
    Crittercism.initialize(eq(context), eq("foo"), any(CrittercismConfig.class));
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityDestroyed(activity);
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void identify() {
    Traits traits = new Traits().putUserId("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    Crittercism.setUsername("foo");
    verifyStatic();
    Crittercism.setMetadata(jsonEq(traits.toJsonObject()));
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").category("bar").build());
    verifyStatic();
    Crittercism.leaveBreadcrumb("Viewed foo Screen");
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    Crittercism.leaveBreadcrumb("foo");
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyStatic();
    Crittercism.sendAppLoadData();
  }
}
