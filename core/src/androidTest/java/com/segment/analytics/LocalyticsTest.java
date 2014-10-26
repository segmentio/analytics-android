package com.segment.analytics;

import com.localytics.android.LocalyticsSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class LocalyticsTest extends IntegrationExam {
  @Mock LocalyticsSession localytics;
  LocalyticsIntegrationAdapter adapter;

  @Before @Override public void setUp() {
    super.setUp();

    adapter = new LocalyticsIntegrationAdapter() {
      @Override LocalyticsSession getUnderlyingInstance() {
        return localytics;
      }
    };
    assertThat(adapter.getUnderlyingInstance()).isEqualTo(localytics);
  }

  @Test public void initialize() throws InvalidConfigurationException {
    LocalyticsIntegrationAdapter adapter = new LocalyticsIntegrationAdapter();
    adapter.initialize(Robolectric.application, new JsonMap().putValue("appKey", "foo"));
    assertThat(adapter.localyticsSession).isNotNull();
  }

  @Test
  public void activityLifecycle() {
    adapter.onActivityResumed(activity);
    verify(localytics).open();
    adapter.onActivityPaused(activity);
    verify(localytics).close();
    // Ignored
    adapter.onActivityCreated(activity, bundle);
    adapter.onActivityStarted(activity);
    adapter.onActivityDestroyed(activity);
    adapter.onActivitySaveInstanceState(activity, bundle);
    adapter.onActivityStopped(activity);
    verifyNoMoreInteractions(localytics);
  }

  @Test public void flush() {
    adapter.flush();
    verify(localytics).upload();
  }

  @Test public void optOut() {
    adapter.optOut(false);
    verify(localytics).setOptOut(false);

    adapter.optOut(true);
    verify(localytics).setOptOut(true);
  }

  @Test public void screen() {
    adapter.screen(screenPayload("Clothes", "Pants"));
    verify(localytics).tagScreen("Pants");

    adapter.screen(screenPayload(null, "Shirts"));
    verify(localytics).tagScreen("Shirts");

    adapter.screen(screenPayload("Games", null));
    verify(localytics).tagScreen("Games");
  }

  @Test public void track() {
    adapter.track(trackPayload("Button Clicked"));
    verify(localytics).tagEvent("Button Clicked", properties.toStringMap());
  }
}

