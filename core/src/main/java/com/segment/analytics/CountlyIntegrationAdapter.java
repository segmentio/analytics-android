package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import ly.count.android.api.Countly;

import static com.segment.analytics.Utils.nullOrDefault;

/**
 * Countly is a general-purpose analytics tool for your mobile apps, with reports like traffic
 * sources, demographics, event tracking and segmentation.
 *
 * @see <a href="https://count.ly/">Countly</a>
 * @see <a href="https://segment.io/docs/integrations/countly/">Countly Integration</a>
 * @see <a href="https://github.com/Countly/countly-sdk-android">Countly Android SDK</a>
 */
class CountlyIntegrationAdapter extends AbstractIntegrationAdapter<Countly> {

  @Override void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    Countly.sharedInstance()
        .init(context, settings.getString("serverUrl"), settings.getString("appKey"));
  }

  @Override Countly getUnderlyingInstance() {
    return Countly.sharedInstance();
  }

  @Override String className() {
    return "ly.count.android.api.Countly";
  }

  @Override String key() {
    return "Countly";
  }

  @Override void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    Countly.sharedInstance().onStart();
  }

  @Override void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    Countly.sharedInstance().onStop();
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
    Integer count = properties.getInteger("count");
    Double sum = properties.getDouble("sum");
    Countly.sharedInstance()
        .recordEvent(name, properties.toStringMap(), nullOrDefault(count, 1),
            nullOrDefault(sum, 0d));
  }
}
