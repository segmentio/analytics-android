package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.GroupPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.HashMap;
import ly.count.android.api.Countly;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.content.Context.MODE_PRIVATE;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class CountlyTest {

  @Mock Countly countly;
  @Mock Application context;
  CountlyIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    integration = new CountlyIntegration();
    integration.countly = countly;
    assertThat(integration.getUnderlyingInstance()).isNotNull().isEqualTo(countly);
  }

  @Test public void initialize() throws IllegalStateException {
    integration.countly = null;
    SharedPreferences countlyPrefs =
        RuntimeEnvironment.application.getSharedPreferences("countly", MODE_PRIVATE);
    when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(countlyPrefs);
    integration.initialize(context, new ValueMap() //
        .putValue("serverUrl", "https://countly.com") //
        .putValue("appKey", "foo"), NONE);
    assertThat(integration.countly).isNotNull();
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(countly);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verify(countly).onStart();
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(countly).onStop();
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(countly);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test public void identify() {
    integration.identify(mock(IdentifyPayload.class));
    verifyNoMoreInteractions(countly);
  }

  @Test public void group() {
    integration.group(mock(GroupPayload.class));
    verifyNoMoreInteractions(countly);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder() //
        .event("foo") //
        .build());
    verify(countly).recordEvent("foo", new HashMap<String, String>(), 1, 0.0);

    Properties properties = new Properties().putValue("count", 10).putValue("sum", 20);
    integration.track(new TrackPayloadBuilder().event("bar").properties(properties).build());
    verify(countly).recordEvent("bar", properties.toStringMap(), 10, 20);
  }

  @Test public void alias() {
    integration.alias(mock(AliasPayload.class));
    verifyNoMoreInteractions(countly);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verify(countly).recordEvent("Viewed foo Screen", new HashMap<String, String>(), 1, 0.0);

    Properties properties = new Properties().putValue("count", 10).putValue("sum", 20);
    integration.screen(new ScreenPayloadBuilder().name("bar").properties(properties).build());
    verify(countly).recordEvent("Viewed bar Screen", properties.toStringMap(), 10, 20.0);
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(countly);
  }
}
