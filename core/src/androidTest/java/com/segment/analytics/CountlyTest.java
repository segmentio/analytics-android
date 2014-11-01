package com.segment.analytics;

import ly.count.android.api.Countly;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.AbstractIntegrationAdapter.VIEWED_EVENT_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class CountlyTest extends IntegrationExam {
  @Mock Countly countly;
  CountlyIntegrationAdapter countlyIntegrationAdapter;

  @Before @Override public void setUp() {
    super.setUp();

    countlyIntegrationAdapter = new CountlyIntegrationAdapter(true) {
      @Override Countly getUnderlyingInstance() {
        return countly;
      }
    };
    assertThat(countlyIntegrationAdapter.getUnderlyingInstance()).isNotNull().isEqualTo(countly);
  }

  @Test
  public void activityLifecycle() {
    countlyIntegrationAdapter.onActivityStarted(activity);
    verify(countly).onStart();
    countlyIntegrationAdapter.onActivityStopped(activity);
    verify(countly).onStop();
    // Ignored
    countlyIntegrationAdapter.onActivityCreated(activity, bundle);
    countlyIntegrationAdapter.onActivityDestroyed(activity);
    countlyIntegrationAdapter.onActivitySaveInstanceState(activity, bundle);
    countlyIntegrationAdapter.onActivityResumed(activity);
    countlyIntegrationAdapter.onActivityPaused(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test
  public void initialize() throws InvalidConfigurationException {
    countlyIntegrationAdapter.initialize(context,
        new JsonMap().putValue("serverUrl", "foo").putValue("appKey", "bar"));
    verify(countly).init(context, "foo", "bar");
  }

  @Test
  public void track() {
    countlyIntegrationAdapter.track(trackPayload("Button Clicked"));
    verify(countly).recordEvent("Button Clicked", properties.toStringMap(), 1, 0);
    properties.putValue("count", 10);
    properties.putValue("sum", 20);
    countlyIntegrationAdapter.track(trackPayload("Button Clicked"));
    verify(countly).recordEvent("Button Clicked", properties.toStringMap(), 10, 20);
  }

  @Test
  public void screen() {
    countlyIntegrationAdapter.screen(screenPayload("Signup", null));
    verify(countly).recordEvent(String.format(VIEWED_EVENT_FORMAT, "Signup"),
        properties.toStringMap(), 1, 0);

    properties.putValue("count", 10).putValue("sum", 20);
    countlyIntegrationAdapter.screen(screenPayload("Signup", null));
    verify(countly).recordEvent(String.format(VIEWED_EVENT_FORMAT, "Signup"),
        properties.toStringMap(), 10, 20);
  }
}
