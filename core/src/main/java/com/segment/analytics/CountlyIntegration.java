package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import ly.count.android.api.Countly;

/**
 * Countly is a general-purpose analytics tool for your mobile apps, with reports like traffic
 * sources, demographics, event tracking and segmentation.
 *
 * @see <a href="https://count.ly/">Countly</a>
 * @see <a href="https://segment.io/docs/integrations/countly/">Countly Integration</a>
 * @see <a href="https://github.com/Countly/countly-sdk-android">Countly Android SDK</a>
 */
class CountlyIntegration extends AbstractIntegration<Countly> {
  static final String COUNTLY_KEY = "Countly";
  Countly countly;

  @Override void initialize(Context context, JsonMap settings, boolean debuggingEnabled)
      throws InvalidConfigurationException {
    countly = Countly.sharedInstance();
    countly.init(context, settings.getString("serverUrl"), settings.getString("appKey"));
  }

  @Override Countly getUnderlyingInstance() {
    return countly;
  }

  @Override String key() {
    return COUNTLY_KEY;
  }

  @Override void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    countly.onStart();
  }

  @Override void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    countly.onStop();
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    event(track.event(), track.properties());
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
  }

  private void event(String name, Properties properties) {
    int count = properties.getInt("count", 1);
    double sum = properties.getDouble("sum", 0);
    countly.recordEvent(name, properties.toStringMap(), count, sum);
  }
}
