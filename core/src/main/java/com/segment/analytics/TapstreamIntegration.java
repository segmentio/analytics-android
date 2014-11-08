package com.segment.analytics;

import android.app.Application;
import android.content.Context;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;
import java.util.Map;

import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * Tapstream is a mobile attribution tool that lets you attribute app installs to individual users
 * who have visited your website, so your marketing team can know what's working.
 *
 * @see <a href="https://tapstream.com">Tapstream</a>
 * @see <a href="https://segment.io/docs/integrations/tapstream/">Tapstream Integration</a>
 * @see <a href="https://tapstream.com/developer/android-sdk-documentation/">Tapstream Android
 * SDK</a>
 */
class TapstreamIntegration extends AbstractIntegration<Tapstream> {
  static final String TAPSTREAM_KEY = "Tapstream";
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;
  Tapstream tapstream;
  Config config;

  private static Event makeEvent(String name, Properties properties) {
    Event event = new Event(name, false);
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      event.addPair(entry.getKey(), entry.getValue());
    }
    return event;
  }

  @Override void initialize(Context context, JsonMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    trackAllPages = settings.getBoolean("trackAllPages", true);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", true);
    trackNamedPages = settings.getBoolean("trackNamedPages", true);
    config = new Config();
    Tapstream.create((Application) context.getApplicationContext(),
        settings.getString("accountName"), settings.getString("sdkSecret"), config);
    tapstream = Tapstream.getInstance();
  }

  @Override Tapstream getUnderlyingInstance() {
    return tapstream;
  }

  @Override String key() {
    return TAPSTREAM_KEY;
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    tapstream.fireEvent(makeEvent(track.event(), track.properties()));
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    if (trackAllPages) {
      tapstream.fireEvent(makeEvent("screen-" + screen.event(), screen.properties()));
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      tapstream.fireEvent(makeEvent("screen-" + screen.category(), screen.properties()));
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      tapstream.fireEvent(makeEvent("screen-" + screen.name(), screen.properties()));
    }
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    for (Map.Entry<String, Object> entry : identify.traits().entrySet()) {
      config.globalEventParams.put(entry.getKey(), entry.getValue());
    }
  }
}
