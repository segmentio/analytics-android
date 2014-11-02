package com.segment.analytics;

import android.app.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.mockApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class AnalyticsRobolectricTest {
  Application application;
  @Mock Dispatcher dispatcher;
  @Mock IntegrationManager integrationManager;
  @Mock Stats stats;
  @Mock TraitsCache traitsCache;
  @Mock AnalyticsContext analyticsContext;
  @Mock Options defaultOptions;

  private Analytics analytics;

  @Before public void setUp() {
    initMocks(this);
    application = mockApplication();
    Traits traits = new Traits();
    when(traitsCache.get()).thenReturn(traits);
    analytics = new Analytics(application, dispatcher, integrationManager, stats, traitsCache,
        analyticsContext, defaultOptions, true);
  }

  @Test public void logoutClearsTraitsAndUpdatesContext() {
    analytics.logout();
    verify(traitsCache).delete(application);
    verify(analyticsContext).putTraits(traitsCache.get());
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
    verify(dispatcher).dispatchEnqueue(payload);
    verify(integrationManager).submit(payload);
  }

  @Test public void flushInvokesFlushes() throws Exception {
    analytics.flush();
    verify(integrationManager).flush();
    verify(dispatcher).dispatchFlush();
  }

  @Test public void shutdown() {
    assertThat(analytics.shutdown).isFalse();
    analytics.shutdown();
    verify(integrationManager).shutdown();
    verify(stats).shutdown();
    verify(dispatcher).shutdown();
    assertThat(analytics.shutdown).isTrue();
  }

  @Test public void shutdownTwice() {
    assertThat(analytics.shutdown).isFalse();
    analytics.shutdown();
    analytics.shutdown();
    verify(integrationManager).shutdown();
    verify(stats).shutdown();
    verify(dispatcher).shutdown();
    assertThat(analytics.shutdown).isTrue();
  }

  /*
  @Test public void shutdownDisallowedOnSingletonInstance() throws Exception {
    try {
      Analytics analytics = Analytics.with(Robolectric.application);
      analytics.shutdown();
      fail("Calling shutdown() on static singleton instance should throw");
    } catch (UnsupportedOperationException expected) {
    }
  }
  */

  @Test public void getSnapshotInvokesStats() throws Exception {
    analytics.getSnapshot();
    verify(stats).createSnapshot();
  }
}
