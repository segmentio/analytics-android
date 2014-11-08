package com.segment.analytics;

import ly.count.android.api.Countly;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.AbstractIntegration.VIEWED_EVENT_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class CountlyRobolectricTest extends IntegrationRobolectricExam {
  @Mock Countly countly;
  CountlyIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();

    integration = new CountlyIntegration();
    integration.countly = countly;
    assertThat(integration.getUnderlyingInstance()).isNotNull().isEqualTo(countly);
  }

  @Test
  public void activityLifecycle() {
    integration.onActivityStarted(activity);
    verify(countly).onStart();
    integration.onActivityStopped(activity);
    verify(countly).onStop();
    // Ignored
    integration.onActivityCreated(activity, bundle);
    integration.onActivityDestroyed(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    integration.onActivityResumed(activity);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test
  public void initialize() throws IllegalStateException {
    try {
      integration.initialize(context,
          new JsonMap().putValue("serverUrl", "foo").putValue("appKey", "bar"), true);
    } catch (NullPointerException ignored) {
      // http://pastebin.com/jHRZyhr7
    }
  }

  @Test
  public void track() {
    integration.track(trackPayload("Button Clicked"));
    verify(countly).recordEvent("Button Clicked", properties.toStringMap(), 1, 0);
    properties.putValue("count", 10);
    properties.putValue("sum", 20);
    integration.track(trackPayload("Button Clicked"));
    verify(countly).recordEvent("Button Clicked", properties.toStringMap(), 10, 20);
  }

  @Test
  public void screen() {
    integration.screen(screenPayload("Signup", null));
    verify(countly).recordEvent(String.format(VIEWED_EVENT_FORMAT, "Signup"),
        properties.toStringMap(), 1, 0);

    properties.putValue("count", 10).putValue("sum", 20);
    integration.screen(screenPayload("Signup", null));
    verify(countly).recordEvent(String.format(VIEWED_EVENT_FORMAT, "Signup"),
        properties.toStringMap(), 10, 20);
  }
}
