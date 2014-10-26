package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import ly.count.android.api.Countly;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.AbstractIntegrationAdapter.VIEWED_EVENT_FORMAT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class CountlyTest {
  CountlyIntegrationAdapter countlyIntegrationAdapter;
  @Mock Countly countly;
  @Mock Activity activity;
  @Mock AnalyticsContext analyticsContext;

  @Before
  public void setUp() {
    initMocks(this);
    countlyIntegrationAdapter = new CountlyIntegrationAdapter() {
      @Override Countly getUnderlyingInstance() {
        return countly;
      }
    };
  }

  @Test
  public void onActivityStartedCalledCorrectly() throws Exception {
    countlyIntegrationAdapter.onActivityStarted(activity);
    verify(countly).onStart();
  }

  @Test
  public void onActivityStoppedCalledCorrectly() throws Exception {
    countlyIntegrationAdapter.onActivityStopped(activity);
    verify(countly).onStop();
  }

  @Test
  public void activityLifecycleEventsIgnored() throws Exception {
    countlyIntegrationAdapter.onActivityCreated(activity, new Bundle());
    countlyIntegrationAdapter.onActivityDestroyed(activity);
    countlyIntegrationAdapter.onActivitySaveInstanceState(activity, new Bundle());
    countlyIntegrationAdapter.onActivityResumed(activity);
    countlyIntegrationAdapter.onActivityPaused(activity);
    verifyNoMoreInteractions(countly);
  }

  @Test
  public void initializedCorrectly() throws Exception {
    countlyIntegrationAdapter.initialize(activity, new JsonMap() {
      @Override String getString(String key) {
        if ("serverUrl".equals(key)) {
          return "serverUrl";
        } else if ("appKey".equals(key)) {
          return "appKey";
        }
        throw new UnsupportedOperationException("unknown key! " + key);
      }
    });
    verify(countly).init(activity, "serverUrl", "appKey");
  }

  @Test
  public void trackIsTranslatedCorrectly() throws Exception {
    Properties properties = new Properties();
    countlyIntegrationAdapter.track(
        new TrackPayload("anonymousId", analyticsContext, "userId", "test", properties,
            new Options())
    );
    verify(countly).recordEvent("test", properties.toStringMap(), 1, 0);

    properties.putValue("count", 10);
    properties.putValue("sum", 20);
    countlyIntegrationAdapter.track(
        new TrackPayload("anonymousId", analyticsContext, "userId", "test", properties,
            new Options())
    );
    verify(countly).recordEvent("test", properties.toStringMap(), 10, 20);
  }

  @Test
  public void screenIsTranslatedCorrectly() throws Exception {
    Properties properties = new Properties();
    countlyIntegrationAdapter.screen(
        new ScreenPayload("anonymousId", analyticsContext, "userId", "category", "test", properties,
            new Options())
    );
    verify(countly).recordEvent(String.format(VIEWED_EVENT_FORMAT, "test"),
        properties.toStringMap(), 1, 0);

    properties.putValue("count", 10);
    properties.putValue("sum", 20);
    countlyIntegrationAdapter.screen(
        new ScreenPayload("anonymousId", analyticsContext, "userId", "category", "test", properties,
            new Options())
    );
    verify(countly).recordEvent(String.format(VIEWED_EVENT_FORMAT, "test"),
        properties.toStringMap(), 10, 20);
  }
}
