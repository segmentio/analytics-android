package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import java.util.HashMap;
import ly.count.android.api.Countly;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class CountlyRobolectricTest extends AbstractIntegrationTest {
  @Mock Countly countly;
  CountlyIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    integration = new CountlyIntegration();
    integration.countly = countly;
    assertThat(integration.getUnderlyingInstance()).isNotNull().isEqualTo(countly);
  }

  @Test @Override public void initialize() throws IllegalStateException {
    try {
      integration.initialize(context,
          new JsonMap().putValue("serverUrl", "foo").putValue("appKey", "bar"), true);
    } catch (NullPointerException ignored) {
      // an NPE occurs in Countly's SDK, but we only need to verify that we did indeed call the SDK
      // correctly
      // http://pastebin.com/jHRZyhr7
    }
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(countly);
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verify(countly).onStart();
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(countly).onStop();
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(countly);
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test @Override public void identify() {
    integration.identify(mock(IdentifyPayload.class));
    verifyNoMoreInteractions(countly);
  }

  @Test @Override public void group() {
    integration.group(mock(GroupPayload.class));
    verifyNoMoreInteractions(countly);
  }

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder() //
        .event("foo") //
        .build());
    verify(countly).recordEvent("foo", new HashMap<String, String>(), 1, 0.0);

    Properties properties = new Properties().putValue("count", 10).putValue("sum", 20);
    integration.track(new TrackPayloadBuilder().event("bar").properties(properties).build());
    verify(countly).recordEvent("bar", properties.toStringMap(), 10, 20);
  }

  @Test @Override public void alias() {
    integration.alias(mock(AliasPayload.class));
    verifyNoMoreInteractions(countly);
  }

  @Test @Override public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verify(countly).recordEvent("Viewed foo Screen", new HashMap<String, String>(), 1, 0.0);

    Properties properties = new Properties().putValue("count", 10).putValue("sum", 20);
    integration.screen(new ScreenPayloadBuilder().name("bar").properties(properties).build());
    verify(countly).recordEvent("Viewed bar Screen", properties.toStringMap(), 10, 20.0);
  }

  @Test @Override public void flush() {
    integration.flush();
    verifyNoMoreInteractions(countly);
  }
}
