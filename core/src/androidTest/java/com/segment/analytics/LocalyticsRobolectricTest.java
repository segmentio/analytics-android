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
public class LocalyticsRobolectricTest extends IntegrationRobolectricExam {
  @Mock LocalyticsAmpSession localytics;
  LocalyticsIntegration integration;

  public static void grantPermission(final Application app, final String permission) {
    ShadowApplication shadowApp = Robolectric.shadowOf(app);
    shadowApp.grantPermissions(permission);
  }

  @Before @Override public void setUp() {
    super.setUp();

    grantPermission(Robolectric.application, Manifest.permission.WAKE_LOCK);

    integration = new LocalyticsIntegration();
    integration.session = localytics;
  }

  @Test public void initialize() throws IllegalStateException {
    LocalyticsIntegration adapter = new LocalyticsIntegration();
    adapter.initialize(Robolectric.application, new JsonMap().putValue("appKey", "foo"), true);
    assertThat(adapter.session).isNotNull();
  }

  @Test
  public void onActivityResumed() {
    integration.onActivityResumed(activity);
    verify(localytics).open();
    verify(localytics).upload();
    verifyNoMoreInteractions(localytics);
  }

  @Test
  public void onActivityResumedCompat() {
    FragmentActivity activity = mock(FragmentActivity.class);
    integration.onActivityResumed(activity);
    verify(localytics).open();
    verify(localytics).upload();
    verify(localytics).attach(activity);
  }

  @Test
  public void onActivityPaused() {
    integration.onActivityPaused(activity);
    verify(localytics).close();
    verify(localytics).upload();
    verifyNoMoreInteractions(localytics);
  }

  @Test
  public void onActivityPausedCompat() {
    activity = mock(FragmentActivity.class);
    integration.onActivityPaused(activity);
    verify(localytics).detach();
    verify(localytics).close();
    verify(localytics).upload();
  }

  @Test
  public void onActivityCreated() {
    integration.onActivityCreated(activity, bundle);
    verify(localytics).open();
    verify(localytics).upload();
    verifyNoMoreInteractions(localytics);
  }

  @Test
  public void activityLifecycle() {
    integration.onActivityStarted(activity);
    integration.onActivityDestroyed(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(localytics);
  }

  @Test public void flush() {
    integration.flush();
    verify(localytics).upload();
  }

  @Test public void screen() {
    integration.screen(screenPayload("Clothes", "Pants"));
    verify(localytics).tagScreen("Pants");

    integration.screen(screenPayload(null, "Shirts"));
    verify(localytics).tagScreen("Shirts");

    integration.screen(screenPayload("Games", null));
    verify(localytics).tagScreen("Games");
  }

  @Test public void track() {
    integration.track(trackPayload("Button Clicked"));
    verify(localytics).tagEvent("Button Clicked", properties.toStringMap());
  }
}

