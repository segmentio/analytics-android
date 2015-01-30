package com.segment.analytics.internal.integrations;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Logger;
import com.tapstream.sdk.Logging;
import com.tapstream.sdk.Tapstream;
import java.util.Map;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Tapstream is a mobile attribution tool that lets you attribute app installs to individual users
 * who have visited your website, so your marketing team can know what's working.
 *
 * @see <a href="https://tapstream.com">Tapstream</a>
 * @see <a href="https://segment.com/docs/integrations/tapstream/">Tapstream Integration</a>
 * @see <a href="https://tapstream.com/developer/android-sdk-documentation/">Tapstream Android
 * SDK</a>
 */
public class TapstreamIntegration extends AbstractIntegration<Tapstream> {
  static final String TAPSTREAM_KEY = "Tapstream";
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;
  Tapstream tapstream;
  Config config;

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    trackAllPages = settings.getBoolean("trackAllPages", true);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", true);
    trackNamedPages = settings.getBoolean("trackNamedPages", true);

    if (logLevel == INFO || logLevel == VERBOSE) {
      Logging.setLogger(new Logger() {
        @Override public void log(int i, String s) {
          Log.d(TAPSTREAM_KEY, s);
        }
      });
    }

    config = new Config();
    Tapstream.create((Application) context.getApplicationContext(),
        settings.getString("accountName"), settings.getString("sdkSecret"), config);
    tapstream = Tapstream.getInstance();
  }

  @Override public Tapstream getUnderlyingInstance() {
    return tapstream;
  }

  @Override public String key() {
    return TAPSTREAM_KEY;
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    tapstream.fireEvent(makeEvent(track.event(), track.properties()));
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    if (trackAllPages) {
      tapstream.fireEvent(
          makeEvent(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties()));
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      tapstream.fireEvent(
          makeEvent(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties()));
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      tapstream.fireEvent(
          makeEvent(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties()));
    }
  }

  private Event makeEvent(String name, Properties properties) {
    Event event = new Event(name, false);
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      event.addPair(entry.getKey(), entry.getValue());
    }
    return event;
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    for (Map.Entry<String, Object> entry : identify.traits().entrySet()) {
      config.globalEventParams.put(entry.getKey(), entry.getValue());
    }
  }
}
