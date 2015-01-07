package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.localytics.android.LocalyticsAmpSession;
import com.localytics.android.LocalyticsSession;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.HashMap;
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
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class LocalyticsTest {
  @Mock LocalyticsAmpSession session;
  @Mock Application context;
  LocalyticsIntegration integration;

  public static void grantPermission(final Application app, final String permission) {
    ShadowApplication shadowApp = Robolectric.shadowOf(app);
    shadowApp.grantPermissions(permission);
  }

  @Before public void setUp() {
    initMocks(this);
    grantPermission(Robolectric.application, Manifest.permission.WAKE_LOCK);
    integration = new LocalyticsIntegration();
    integration.session = session;
    integration.hasSupportLibOnClassPath = true;
  }

  @Test public void initialize() throws IllegalStateException {
    LocalyticsIntegration integration = new LocalyticsIntegration();
    integration.initialize(Robolectric.application, new ValueMap().putValue("appKey", "foo"), true);
    assertThat(integration.session).isNotNull();
    assertThat(LocalyticsSession.isLoggingEnabled()).isTrue();

    integration.initialize(Robolectric.application, new ValueMap().putValue("appKey", "foo"),
        false);
    assertThat(LocalyticsSession.isLoggingEnabled()).isFalse();
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verify(session).open();
    verify(session).upload();
    verifyNoMoreInteractions(session);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(session);
  }

  @Test public void activityResume() {
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

  @Test public void activityPause() {
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

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(session);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(session);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(session);
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder().build());
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(session);
  }

  @Test public void flush() {
    integration.flush();
    verify(session).upload();
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("foo").name("bar").build());
    verify(session).tagScreen("bar");

    integration.screen(new ScreenPayloadBuilder().name("baz").build());
    verify(session).tagScreen("baz");

    integration.screen(new ScreenPayloadBuilder().category("qux").build());
    verify(session).tagScreen("qux");
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(session).tagEvent("foo", new HashMap<String, String>());
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(session);
  }
}

