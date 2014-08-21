package com.segment.android.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.quantcast.measurement.service.QuantcastClient;
import com.segment.android.internal.Logger;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.settings.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.Map;

import static com.segment.android.internal.Utils.hasPermission;

public class QuantcastIntegration extends AbstractIntegration<Void> {
  String apiKey;

  public QuantcastIntegration() throws ClassNotFoundException {
    super("Quantcast", "com.quantcast.measurement.service.QuantcastClient");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new InvalidConfigurationException("ACCESS_NETWORK_STATE is required");
    }
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    QuantcastSettings settings = new QuantcastSettings(projectSettings.getJsonMap(key()));
    apiKey = settings.apiKey();
    QuantcastClient.enableLogging(Logger.isLogging());
    return true;
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    QuantcastClient.activityStart(activity, apiKey, null, null);
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    QuantcastClient.activityStop();
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    QuantcastClient.recordUserIdentifier(identify.userId());
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    event("Viewed " + screen.name() + " Screen");
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    event(track.event());
  }

  private void event(String name) {
    QuantcastClient.logEvent(name);
  }

  static class QuantcastSettings extends JsonMap {
    QuantcastSettings(Map<String, Object> delegate) {
      super(delegate);
    }

    String apiKey() {
      return getString("apiKey");
    }

    String pCode() {
      return getString("pCode");
    }

    String advertise() {
      return getString("advertise");
    }
  }
}
