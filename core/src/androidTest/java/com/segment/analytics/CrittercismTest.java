package com.segment.analytics;

import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.AbstractIntegrationAdapter.VIEWED_EVENT_FORMAT;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Crittercism.class)
public class CrittercismTest extends IntegrationExam {
  @Rule public PowerMockRule rule = new PowerMockRule();

  CrittercismIntegrationAdapter adapter;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Crittercism.class);
    adapter = new CrittercismIntegrationAdapter();
  }

  @Test public void initialize() throws Exception {
    adapter.initialize(context, new JsonMap().putValue("appId", "foo"));
    verifyStatic();
    // todo: verify config params
    Crittercism.initialize(eq(context), eq("foo"), Matchers.<CrittercismConfig>any());
  }

  @Test public void activityLifecycle() {
    adapter.onActivityResumed(activity);
    adapter.onActivityPaused(activity);
    adapter.onActivityStarted(activity);
    adapter.onActivityStopped(activity);
    adapter.onActivityCreated(activity, bundle);
    adapter.onActivityDestroyed(activity);
    adapter.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void identify() {
    adapter.identify(identifyPayload("foo"));
    verifyStatic();
    Crittercism.setUsername("foo");
    verifyStatic();
    Crittercism.setMetadata(Matchers.<JSONObject>any());
  }

  @Test public void screen() {
    adapter.screen(screenPayload("foo", "bar"));
    verifyStatic();
    Crittercism.leaveBreadcrumb(String.format(VIEWED_EVENT_FORMAT, "bar"));
  }

  @Test public void track() {
    adapter.track(trackPayload("foo"));
    verifyStatic();
    Crittercism.leaveBreadcrumb("foo");
  }

  @Test public void flush() {
    adapter.flush();
    verifyStatic();
    Crittercism.sendAppLoadData();
  }

  @Test public void optOut() {
    adapter.optOut(false);
    verifyStatic();
    Crittercism.setOptOutStatus(false);

    adapter.optOut(true);
    verifyStatic();
    Crittercism.setOptOutStatus(true);
  }
}
