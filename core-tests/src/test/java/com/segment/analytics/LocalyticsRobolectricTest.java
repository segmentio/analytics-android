package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.localytics.android.LocalyticsAmpSession;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.segment.analytics.TestUtils.AliasPayloadBuilder;
import static com.segment.analytics.TestUtils.GroupPayloadBuilder;
import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class LocalyticsRobolectricTest extends AbstractIntegrationTest {
  @Mock LocalyticsAmpSession session;
  LocalyticsIntegration integration;

  public static void grantPermission(final Application app, final String permission) {
    ShadowApplication shadowApp = Robolectric.shadowOf(app);
    shadowApp.grantPermissions(permission);
  }

  @Before @Override public void setUp() {
    super.setUp();
    grantPermission(Robolectric.application, Manifest.permission.WAKE_LOCK);
    integration = new LocalyticsIntegration();
    integration.session = session;
  }

  @Test @Override public void initialize() throws IllegalStateException {
    LocalyticsIntegration integration = new LocalyticsIntegration();
    integration.initialize(Robolectric.application, new ValueMap().putValue("appKey", "foo"), true);
    assertThat(integration.session).isNotNull();
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verify(session).open();
    verify(session).upload();
    verifyNoMoreInteractions(session);
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(session);
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verify(session).open();
    verify(session).upload();
    verifyNoMoreInteractions(session);
  }

  @Test public void activityResumeCompat() {
    FragmentActivity activity = mock(FragmentActivity.class);
    integration.onActivityResumed(activity);
    verify(session).open();
    verify(session).upload();
    verify(session).attach(activity);
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verify(session).close();
    verify(session).upload();
    verifyNoMoreInteractions(session);
  }

  @Test public void activityPauseCompat() {
    FragmentActivity activity = mock(FragmentActivity.class);
    integration.onActivityPaused(activity);
    verify(session).detach();
    verify(session).close();
    verify(session).upload();
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(session);
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(session);
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(session);
  }

  @Test @Override public void identify() {
    integration.identify(new IdentifyPayloadBuilder().build());
  }

  @Test @Override public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(session);
  }

  @Test @Override public void flush() {
    integration.flush();
    verify(session).upload();
  }

  @Test @Override public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("foo").name("bar").build());
    verify(session).tagScreen("bar");

    integration.screen(new ScreenPayloadBuilder().name("baz").build());
    verify(session).tagScreen("baz");

    integration.screen(new ScreenPayloadBuilder().category("qux").build());
    verify(session).tagScreen("qux");
  }

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(session).tagEvent("foo", new HashMap<String, String>());
  }

  @Test @Override public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(session);
  }
}

