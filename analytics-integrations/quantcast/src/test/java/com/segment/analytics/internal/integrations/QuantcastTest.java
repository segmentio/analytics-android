package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.quantcast.measurement.service.QuantcastClient;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
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

import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(QuantcastClient.class)
public class QuantcastTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  QuantcastIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(QuantcastClient.class);
    integration = new QuantcastIntegration();
    integration.apiKey = "foo";
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("apiKey", "foo"), NONE);
    verifyStatic();
    QuantcastClient.enableLogging(false);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void initializeWithDebuggingInfo() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("apiKey", "foo"), INFO);
    verifyStatic();
    QuantcastClient.enableLogging(true);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void initializeWithDebuggingFull() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("apiKey", "foo"), VERBOSE);
    verifyStatic();
    QuantcastClient.enableLogging(true);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    QuantcastClient.activityStart(activity, "foo", null, null);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    QuantcastClient.activityStop();
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder() //
        .traits(createTraits("bar")).build());
    verifyStatic();
    QuantcastClient.recordUserIdentifier("bar");
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("bar").build());
    verifyStatic();
    QuantcastClient.logEvent("bar");
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("bar").build());
    verifyStatic();
    QuantcastClient.logEvent("Viewed bar Screen");

    integration.screen(new ScreenPayloadBuilder().name("baz").build());
    verifyStatic();
    QuantcastClient.logEvent("Viewed baz Screen");
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(QuantcastClient.class);
  }
}
