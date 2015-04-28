package com.segment.analytics.internal.integrations;

import android.app.Activity;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import ly.count.android.api.Countly;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;

/**
 * Countly is a general-purpose analytics tool for your mobile apps, with reports like traffic
 * sources, demographics, event tracking and segmentation.
 *
 * @see <a href="https://count.ly/">Countly</a>
 * @see <a href="https://segment.com/docs/integrations/countly/">Countly Integration</a>
 * @see <a href="https://github.com/Countly/countly-sdk-android">Countly Android SDK</a>
 */
public class CountlyIntegration extends AbstractIntegration<Countly> {

  static final String COUNTLY_KEY = "Countly";
  Countly countly;

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    countly = Countly.sharedInstance();
    LogLevel logLevel = analytics.getLogLevel();
    countly.setLoggingEnabled(logLevel == INFO || logLevel == VERBOSE);
    String serverUrl = settings.getString("serverUrl");
    String appKey = settings.getString("appKey");
    countly.init(analytics.getApplication(), serverUrl, appKey);
  }

  @Override public Countly getUnderlyingInstance() {
    return countly;
  }

  @Override public String key() {
    return COUNTLY_KEY;
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    countly.onStart();
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    countly.onStop();
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    event(track.event(), track.properties());
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
  }

  private void event(String name, Properties properties) {
    int count = properties.getInt("count", 1);
    double sum = properties.getDouble("sum", 0);
    countly.recordEvent(name, properties.toStringMap(), count, sum);
  }
}
