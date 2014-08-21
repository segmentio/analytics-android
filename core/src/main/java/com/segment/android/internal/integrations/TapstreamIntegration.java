package com.segment.android.internal.integrations;

import android.app.Application;
import android.content.Context;
import com.segment.android.Properties;
import com.segment.android.internal.ProjectSettings;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;
import java.util.Map;

public class TapstreamIntegration extends AbstractIntegration<Void> {
  public TapstreamIntegration() throws ClassNotFoundException {
    super("Tapstream", "com.tapstream.sdk.Tapstream");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // no extra permissions
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    TapstreamSettings settings = new TapstreamSettings(projectSettings.getJsonMap(key()));
    Tapstream.create((Application) context, settings.accountName(), settings.sdkSecret(),
        new Config());
    return true;
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    Tapstream.getInstance().fireEvent(makeEvent(track.event(), track.properties(), track));
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    Tapstream.getInstance()
        .fireEvent(makeEvent("screen-" + screen.name(), screen.properties(), screen));
  }

  private Event makeEvent(String name, Properties properties, BasePayload payload) {
    Event event = new Event(name, false);
    if (properties != null) {
      for (Map.Entry<String, Object> entry : properties.entrySet()) {
        event.addPair(entry.getKey(), entry.getValue());
      }
      // todo: attach context and traits?
    }
    return event;
  }

  static class TapstreamSettings extends JsonMap {
    TapstreamSettings(Map<String, Object> delegate) {
      super(delegate);
    }

    String accountName() {
      return getString("accountName");
    }

    String sdkSecret() {
      return getString("sdkSecret");
    }

    Boolean trackAllPages() {
      return getBoolean("trackAllPages");
    }

    Boolean trackCategorizedPages() {
      return getBoolean("trackCategorizedPages");
    }

    Boolean trackNamedPages() {
      return getBoolean("trackNamedPages");
    }
  }
}
