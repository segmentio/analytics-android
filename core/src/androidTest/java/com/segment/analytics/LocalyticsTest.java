package com.segment.analytics;

import android.content.Context;
import com.localytics.android.LocalyticsSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class LocalyticsTest extends IntegrationExam {
  @Mock LocalyticsSession localyticsSession;
  LocalyticsIntegrationAdapter localyticsIntegrationAdapter;

  @Before
  public void setUp() {
    super.setUp();
    LocalyticsIntegrationAdapter.LocalyticsSessionFactory factory =
        new LocalyticsIntegrationAdapter.LocalyticsSessionFactory() {
          @Override LocalyticsSession create(Context context, String appKey) {
            return localyticsSession;
          }
        };
    localyticsIntegrationAdapter = new LocalyticsIntegrationAdapter(factory);
    try {
      localyticsIntegrationAdapter.initialize(context, new JsonMap());
    } catch (InvalidConfigurationException e) {
      fail("Must be initialized correctly!");
    }
  }

  @Test public void initialize() throws InvalidConfigurationException {
    LocalyticsIntegrationAdapter adapter = new LocalyticsIntegrationAdapter();
    adapter.initialize(Robolectric.application, new JsonMap().putValue("appKey", "foo"));
    assertThat(adapter.localyticsSession).isNotNull();
  }

  @Test
  public void activityLifecycle() {
    localyticsIntegrationAdapter.onActivityResumed(activity);
    verify(localyticsSession).open();
    localyticsIntegrationAdapter.onActivityPaused(activity);
    verify(localyticsSession).close();
    // Ignored
    localyticsIntegrationAdapter.onActivityCreated(activity, bundle);
    localyticsIntegrationAdapter.onActivityStarted(activity);
    localyticsIntegrationAdapter.onActivityDestroyed(activity);
    localyticsIntegrationAdapter.onActivitySaveInstanceState(activity, bundle);
    localyticsIntegrationAdapter.onActivityStopped(activity);
    verifyNoMoreInteractions(localyticsSession);
  }

  @Test public void flush() {
    localyticsIntegrationAdapter.flush();
    verify(localyticsSession).upload();
  }

  @Test public void optOut() {
    localyticsIntegrationAdapter.optOut(false);
    verify(localyticsSession).setOptOut(false);

    localyticsIntegrationAdapter.optOut(true);
    verify(localyticsSession).setOptOut(true);
  }

  @Test public void screen() {
    localyticsIntegrationAdapter.screen(screenPayload("Clothes", "Pants"));
    verify(localyticsSession).tagScreen("Pants");

    localyticsIntegrationAdapter.screen(screenPayload(null, "Shirts"));
    verify(localyticsSession).tagScreen("Shirts");

    localyticsIntegrationAdapter.screen(screenPayload("Games", null));
    verify(localyticsSession).tagScreen("Games");
  }

  @Test public void track() {
    localyticsIntegrationAdapter.track(trackPayload("Button Clicked"));
    verify(localyticsSession).tagEvent("Button Clicked", properties.toStringMap());
  }
}

