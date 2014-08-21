package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;
import com.segment.android.AnalyticsContext;
import com.segment.android.Properties;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.Map;

import static com.segment.android.internal.Utils.isNullOrEmpty;

public class FlurryIntegration extends AbstractIntegration<Void> {
  FlurrySettings settings;

  public FlurryIntegration() throws ClassNotFoundException {
    super("Flurry", "com.flurry.android.FlurryAgent");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // no extra permissions
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    settings = new FlurrySettings(projectSettings.getJsonMap(key()));
    FlurryAgent.setContinueSessionMillis(settings.sessionContinueSeconds());
    FlurryAgent.setCaptureUncaughtExceptions(settings.captureUncaughtExceptions());
    FlurryAgent.setUseHttps(settings.useHttps());
    return true;
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    FlurryAgent.onStartSession(activity, settings.apiKey());
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    FlurryAgent.onEndSession(activity);
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    event(screen.name(), screen.properties());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    event(track.event(), track.properties());
  }

  void event(String name, Properties properties) {
    FlurryAgent.logEvent(name, properties.toStringMap());
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    FlurryAgent.setUserId(identify.userId());
    Traits traits = identify.getTraits();
    Short age = traits.age();
    if (age != null) {
      FlurryAgent.setAge(age);
    }
    String gender = traits.gender();
    if (!isNullOrEmpty(gender)) {
      if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("m")) {
        FlurryAgent.setGender(Constants.MALE);
      } else if (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("f")) {
        FlurryAgent.setGender(Constants.FEMALE);
      } else {
        FlurryAgent.setGender(Constants.UNKNOWN);
      }
    }
    AnalyticsContext.Location location = identify.context().location();
    FlurryAgent.setLocation((float) location.latitude(), (float) location.longitude());
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  static class FlurrySettings extends JsonMap {
    FlurrySettings(Map<String, Object> delegate) {
      super(delegate);
    }

    String apiKey() {
      return getString("apiKey");
    }

    boolean captureUncaughtExceptions() {
      return getBoolean("captureUncaughtExceptions");
    }

    boolean useHttps() {
      return getBoolean("useHttps");
    }

    int sessionContinueSeconds() {
      return getInteger("sessionContinueSeconds");
    }
  }
}
