package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class LocalyticsTest {
  LocalyticsIntegrationAdapter localyticsIntegrationAdapter;
  Context context = Robolectric.application;
  @Mock LocalyticsSession localyticsSession;
  @Mock AnalyticsContext analyticsContext;
  @Mock Activity activity;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    LocalyticsIntegrationAdapter.LocalyticsSessionFactory factory =
        new LocalyticsIntegrationAdapter.LocalyticsSessionFactory() {
          @Override LocalyticsSession create(Context context, String appKey) {
            return localyticsSession;
          }
        };
    localyticsIntegrationAdapter = new LocalyticsIntegrationAdapter(factory) {
      @Override LocalyticsSession getUnderlyingInstance() {
        return localyticsSession;
      }
    };
    localyticsIntegrationAdapter.initialize(context, new JsonMap());
    assertThat(localyticsIntegrationAdapter.localyticsSession).isNotNull();
  }

  @Test public void onActivityResumedCalledCorrectly() {
    localyticsIntegrationAdapter.onActivityResumed(activity);
    verify(localyticsSession).open();
  }

  @Test public void onActivityPausedCalledCorrectly() {
    localyticsIntegrationAdapter.onActivityPaused(activity);
    verify(localyticsSession).close();
  }

  @Test public void activityLifecycleIgnoredCorrectly() {
    localyticsIntegrationAdapter.onActivityCreated(activity, new Bundle());
    localyticsIntegrationAdapter.onActivityStarted(activity);
    localyticsIntegrationAdapter.onActivityDestroyed(activity);
    localyticsIntegrationAdapter.onActivitySaveInstanceState(activity, new Bundle());
    localyticsIntegrationAdapter.onActivityStopped(activity);
    verifyNoMoreInteractions(localyticsSession);
  }

  @Test public void flushCalledCorrectly() {
    localyticsIntegrationAdapter.flush();
    verify(localyticsSession).upload();
  }

  @Test public void optOutCalledCorrectly() {
    localyticsIntegrationAdapter.optOut(false);
    verify(localyticsSession).setOptOut(false);

    localyticsIntegrationAdapter.optOut(true);
    verify(localyticsSession).setOptOut(true);
  }

  @Test public void screenCalledCorrectly() {
    localyticsIntegrationAdapter.screen(
        new ScreenPayload("anonymousId", analyticsContext, "userId", "category", "name",
            new Properties(), new Options())
    );
    localyticsSession.tagScreen("name");

    localyticsIntegrationAdapter.screen(
        new ScreenPayload("anonymousId", analyticsContext, "userId", "category", null,
            new Properties(), new Options())
    );
    localyticsSession.tagScreen("category");
  }

  @Test public void trackCalledCorrectly() {
    Properties properties = new Properties();
    localyticsIntegrationAdapter.track(
        new TrackPayload("anonymousId", analyticsContext, "userId", "test", properties,
            new Options())
    );
    verify(localyticsSession).tagEvent("test", properties.toStringMap());
  }

  @Test public void screenIsTranslatedCorrectly() throws Exception {
    localyticsIntegrationAdapter.screen(
        new ScreenPayload("anonymousId", analyticsContext, "userId", "category", "test",
            new Properties(), new Options())
    );
    verify(localyticsSession).tagScreen("test");

    localyticsIntegrationAdapter.screen(
        new ScreenPayload("anonymousId", analyticsContext, "userId", "category", null,
            new Properties(), new Options())
    );
    verify(localyticsSession).tagScreen("category");
  }
}

