package com.segment.android.internal.integrations;

import android.app.Application;
import android.content.Context;
import com.segment.android.Integration;
import com.segment.android.Properties;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;
import java.util.Map;

import static com.segment.android.internal.Utils.isNullOrEmpty;

/**
 * Tapstream is a mobile attribution tool that lets you attribute app installs to individual users
 * who have visited your website, so your marketing team can know what's working.
 *
 * @see {@link https://tapstream.com}
 * @see {@link https://segment.io/docs/integrations/tapstream/}
 * @see {@link https://tapstream.com/developer/android-sdk-documentation/}
 */
public class TapstreamIntegration extends AbstractIntegration<Tapstream> {
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;
  Tapstream tapstream;
  Config config;

  @Override public Integration provider() {
    return Integration.TAPSTREAM;
  }

  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    trackAllPages = settings.getBoolean("trackAllPages");
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages");
    trackNamedPages = settings.getBoolean("trackNamedPages");
    config = new Config();
    Tapstream.create((Application) context, settings.getString("accountName"),
        settings.getString("sdkSecret"), config);
    tapstream = Tapstream.getInstance();
  }

  @Override public Tapstream getUnderlyingInstance() {
    return tapstream;
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    tapstream.fireEvent(makeEvent(track.event(), track.properties()));
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    if (trackAllPages) {
      tapstream.fireEvent(makeEvent("screen-" + screen.event(), screen.properties()));
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      tapstream.fireEvent(makeEvent("screen-" + screen.category(), screen.properties()));
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      tapstream.fireEvent(makeEvent("screen-" + screen.name(), screen.properties()));
    }
  }

  private static Event makeEvent(String name, Properties properties) {
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
