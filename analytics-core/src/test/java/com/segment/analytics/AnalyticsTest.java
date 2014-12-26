package com.segment.analytics;

import android.Manifest;
import android.app.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.segment.analytics.TestUtils.mockApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class AnalyticsTest {
  Application application;
  @Mock IntegrationManager integrationManager;
  @Mock Segment segment;
  @Mock Stats stats;
  @Mock Traits.Cache traitsCache;
  @Mock AnalyticsContext analyticsContext;
  @Mock Options defaultOptions;
  @Mock Logger logger;

  private Analytics analytics;

  public static void grantPermission(final Application app, final String permission) {
    ShadowApplication shadowApp = Robolectric.shadowOf(app);
    shadowApp.grantPermissions(permission);
  }

  @Before public void setUp() {
    initMocks(this);
    grantPermission(Robolectric.application, Manifest.permission.INTERNET);
    application = mockApplication();
    Traits traits = new Traits();
    when(traitsCache.get()).thenReturn(traits);
    analytics = new Analytics(application, integrationManager, segment, stats, traitsCache,
        analyticsContext, defaultOptions, logger, true);
  }

  @Test public void logoutClearsTraitsAndUpdatesContext() {
    analytics.logout();
    verify(traitsCache).delete();
    verify(traitsCache).set(any(Traits.class));
    verify(analyticsContext).setTraits(traitsCache.get()); // update the traits in context
  }

  @Test public void trackFailsForInvalidEvent() {
    try {
      analytics.track(null);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("event must not be null or empty.");
    }
    try {
      analytics.track("   ");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("event must not be null or empty.");
    }
  }

  @Test public void submitInvokesDispatches() {
    BasePayload payload = mock(BasePayload.class);
    analytics.submit(payload);
    verify(integrationManager).dispatchOperation(payload);
    verify(segment).dispatchEnqueue(payload);
  }

  @Test public void flushInvokesFlushes() throws Exception {
    analytics.flush();
    verify(segment).dispatchFlush(0);
    verify(integrationManager).dispatchFlush();
  }

  @Test public void shutdown() {
    assertThat(analytics.shutdown).isFalse();
    analytics.shutdown();
    verify(integrationManager).shutdown();
    verify(stats).shutdown();
    verify(segment).shutdown();
    assertThat(analytics.shutdown).isTrue();
  }

  @Test public void shutdownTwice() {
    assertThat(analytics.shutdown).isFalse();
    analytics.shutdown();
    analytics.shutdown();
    verify(integrationManager).shutdown();
    verify(stats).shutdown();
    assertThat(analytics.shutdown).isTrue();
  }

  /*
  @Test public void shutdownDisallowedOnSingletonInstance() throws Exception {
    Analytics.singleton = null;
    try {
      Analytics analytics = Analytics.with(context); // todo: mock apiKey in resources
      analytics.shutdown();
      fail("Calling shutdown() on static singleton instance should throw");
    } catch (UnsupportedOperationException expected) {
    }
  }
  */

  @Test public void shutdownDisallowedOnCustomSingletonInstance() throws Exception {
    Analytics.singleton = null;
    try {
      Analytics analytics = new Analytics.Builder(Robolectric.application, "foo").build();
      Analytics.setSingletonInstance(analytics);
      analytics.shutdown();
      fail("Calling shutdown() on static singleton instance should throw");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test public void setSingletonInstanceMayOnlyBeCalledOnce() {
    Analytics.singleton = null;

    Analytics analytics = new Analytics.Builder(Robolectric.application, "foo").build();
    Analytics.setSingletonInstance(analytics);

    try {
      Analytics.setSingletonInstance(analytics);
      fail("Can't set singleton instance twice.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Singleton instance already exists.");
    }
  }

  @Test public void setSingletonInstanceAfterWithFails() {
    Analytics.singleton = null;

    // Create the default singleton instance.
    Analytics.setSingletonInstance(new Analytics.Builder(Robolectric.application, "foo").build());

    Analytics analytics = new Analytics.Builder(Robolectric.application, "foo").build();
    try {
      Analytics.setSingletonInstance(analytics);
      fail("Can't set singleton instance after with().");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Singleton instance already exists.");
    }
  }

  @Test public void setSingleInstanceReturnedFromWith() {
    Analytics.singleton = null;
    Analytics analytics = new Analytics.Builder(Robolectric.application, "foo").build();
    Analytics.setSingletonInstance(analytics);
    assertThat(Analytics.with(Robolectric.application)).isSameAs(analytics);
  }

  @Test public void getSnapshotInvokesStats() throws Exception {
    analytics.getSnapshot();
    verify(stats).createSnapshot();
  }

  @Test public void alias() throws Exception {
    try {
      analytics.alias(null);
      fail("null previous id should throw error");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("previousId must not be null or empty.");
    }
  }
}
