package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
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

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    countly = Countly.sharedInstance();
    countly.setLoggingEnabled(logLevel == INFO || logLevel == VERBOSE);
    countly.init(context, settings.getString("serverUrl"), settings.getString("appKey"));
  }

  @Override public Countly getUnderlyingInstance() {
    return countly;
  }

  @Override public String key() {
    return COUNTLY_KEY;
  }

  @Override public boolean onActivityStarted(Activity activity) {
    countly.onStart();
    return true;
  }

  @Override public boolean onActivityStopped(Activity activity) {
    countly.onStop();
    return true;
  }

  @Override public boolean track(TrackPayload track) {
    return event(track.event(), track.properties());
  }

  @Override public boolean screen(ScreenPayload screen) {
    return event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
  }

  private boolean event(String name, Properties properties) {
    int count = properties.getInt("count", 1);
    double sum = properties.getDouble("sum", 0);
    countly.recordEvent(name, properties.toStringMap(), count, sum);
    return true;
  }
}
