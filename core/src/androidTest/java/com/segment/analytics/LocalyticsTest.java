package com.segment.analytics;

import android.Manifest;
import android.app.Application;
import android.support.v4.app.FragmentActivity;
import com.localytics.android.LocalyticsAmpSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class LocalyticsTest extends IntegrationExam {
  @Mock LocalyticsAmpSession localytics;
  LocalyticsIntegrationAdapter adapter;

  @Before @Override public void setUp() {
    super.setUp();

    grantPermission(Robolectric.application, Manifest.permission.WAKE_LOCK);

    adapter = new LocalyticsIntegrationAdapter();
    adapter.session = localytics;
  }

  public static void grantPermission(final Application app, final String permission) {
    ShadowApplication shadowApp = Robolectric.shadowOf(app);
    shadowApp.grantPermissions(permission);
  }

  @Test public void initialize() throws InvalidConfigurationException {
    LocalyticsIntegrationAdapter adapter = new LocalyticsIntegrationAdapter();
    adapter.initialize(Robolectric.application, new JsonMap().putValue("appKey", "foo"));
    assertThat(adapter.session).isNotNull();
  }

  @Test
  public void onActivityResumed() {
    adapter.onActivityResumed(activity);
    verify(localytics).open();
    verify(localytics).upload();
    verifyNoMoreInteractions(localytics);
  }

  @Test
  public void onActivityResumedCompat() {
    FragmentActivity activity = mock(FragmentActivity.class);
    adapter.onActivityResumed(activity);
    verify(localytics).open();
    verify(localytics).upload();
    verify(localytics).attach(activity);
  }

  @Test
  public void onActivityPaused() {
    adapter.onActivityPaused(activity);
    verify(localytics).close();
    verify(localytics).upload();
    verifyNoMoreInteractions(localytics);
  }

  @Test
  public void onActivityPausedCompat() {
    activity = mock(FragmentActivity.class);
    adapter.onActivityPaused(activity);
    verify(localytics).detach();
    verify(localytics).close();
    verify(localytics).upload();
  }

  @Test
  public void onActivityCreated() {
    adapter.onActivityCreated(activity, bundle);
    verify(localytics).open();
    verify(localytics).upload();
    verifyNoMoreInteractions(localytics);
  }

  @Test
  public void activityLifecycle() {
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

