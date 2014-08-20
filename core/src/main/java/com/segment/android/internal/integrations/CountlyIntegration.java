package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.segment.android.Properties;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.settings.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.Map;
import ly.count.android.api.Countly;

public class CountlyIntegration extends AbstractIntegration<Void> {
  public CountlyIntegration() throws ClassNotFoundException {
    super("Countly", "ly.count.android.api.Countly");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // no extra permissions
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    CountlySettings settings = new CountlySettings(projectSettings.getJsonMap(key()));
    Countly.sharedInstance().init(context, settings.serverUrl(), settings.appKey());
    return true;
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    Countly.sharedInstance().onStart();
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    Countly.sharedInstance().onStop();
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    event(track.event(), track.properties());
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    event("Viewed " + screen.name() + " Screen", screen.properties());
  }

  private void event(String name, Properties properties) {
    Countly.sharedInstance()
        .recordEvent(name, properties.toStringMap(), properties.getInteger("count"),
            properties.getDouble("sum"));
  }

  static class CountlySettings extends JsonMap {
    CountlySettings(Map<String, Object> delegate) {
      super(delegate);
    }

    String appKey() {
      return getString("appKey");
    }

    String serverUrl() {
      return getString("serverUrl");
    }
  }
}
