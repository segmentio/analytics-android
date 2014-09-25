package com.segment.analytics;

import android.app.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18)
public class AnalyticsTest {
  @Mock Application application;
  @Mock Dispatcher dispatcher;
  @Mock IntegrationManager integrationManager;
  @Mock Stats stats;
  @Mock TraitsCache traitsCache;
  @Mock AnalyticsContext analyticsContext;
  @Mock Options defaultOptions;
  @Mock Logger logger;

  private Analytics analytics;

  @Before public void setUp() {
    initMocks(this);

    Traits traits = new Traits();
    when(traitsCache.get()).thenReturn(traits);

    analytics = new Analytics(application, dispatcher, integrationManager, stats, traitsCache,
        analyticsContext, defaultOptions, logger);
  }

  @Test public void logoutClearsTraitsAndUpdatesContext() {
    analytics.logout();

    verify(traitsCache).delete(application);
    verify(analyticsContext).putTraits(traitsCache.get());
  }
}
