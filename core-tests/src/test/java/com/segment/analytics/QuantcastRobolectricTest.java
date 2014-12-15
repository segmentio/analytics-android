package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import com.quantcast.measurement.service.QuantcastClient;
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
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(QuantcastClient.class)
public class QuantcastRobolectricTest extends AbstractIntegrationTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  QuantcastIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(QuantcastClient.class);
    integration = new QuantcastIntegration();
    integration.apiKey = "foo";
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("apiKey", "foo"), false);
    verifyStatic();
    QuantcastClient.enableLogging(false);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void initializeWithDebugging() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("apiKey", "foo"), true);
    verifyStatic();
    QuantcastClient.enableLogging(true);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    QuantcastClient.activityStart(activity, "foo", null, null);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    QuantcastClient.activityStop();
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void identify() {
    integration.identify(new IdentifyPayloadBuilder() //
        .traits(new Traits().putUserId("bar")).build());
    verifyStatic();
    QuantcastClient.recordUserIdentifier("bar");
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder().event("bar").build());
    verifyStatic();
    QuantcastClient.logEvent("bar");
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("bar").build());
    verifyStatic();
    QuantcastClient.logEvent("Viewed bar Screen");

    integration.screen(new ScreenPayloadBuilder().name("baz").build());
    verifyStatic();
    QuantcastClient.logEvent("Viewed baz Screen");
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test @Override public void flush() {
    integration.flush();
    verifyNoMoreInteractions(QuantcastClient.class);
  }
}
