package com.segment.android.integrations;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import com.quantcast.measurement.service.QuantcastClient;
import com.segment.android.Constants;
import com.segment.android.Logger;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;

public class QuantcastIntegration extends SimpleIntegration {

  private static class SettingKey {

    private static final String API_KEY = "apiKey";
  }

  private String apiKey;

  @Override
  public String getKey() {
    return "Quantcast";
  }

  @Override
  public String[] getRequiredPermissions() {
    return new String[] {
        Constants.Permission.ACCESS_NETWORK_STATE
    };
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {

    if (TextUtils.isEmpty(settings.getString(SettingKey.API_KEY))) {
      throw new InvalidSettingsException(SettingKey.API_KEY,
          "Quantcast requires the apiKey setting.");
    }
  }

  @Override
  public void onCreate(Context context) {
    checkPermission(context);

    EasyJSONObject settings = this.getSettings();
    apiKey = settings.getString(SettingKey.API_KEY);

    QuantcastClient.enableLogging(Logger.isLogging());

    ready();
  }

  @Override
  public void onActivityStart(Activity activity) {
    if (!checkPermission(activity)) return;
    QuantcastClient.activityStart(activity, apiKey, null, null);
  }

  @Override
  public void onActivityStop(Activity activity) {
    if (!checkPermission(activity)) return;
    QuantcastClient.activityStop();
  }

  @Override
  public void identify(Identify identify) {
    if (!hasPermission) return;
    String userId = identify.getUserId();
    QuantcastClient.recordUserIdentifier(userId);
  }

  @Override
  public void screen(Screen screen) {
    if (!hasPermission) return;
    event("Viewed " + screen.getName() + " Screen", screen.getProperties());
  }

  @Override
  public void track(Track track) {
    if (!hasPermission) return;
    event(track.getEvent(), track.getProperties());
  }

  private void event(String name, Props properties) {
    QuantcastClient.logEvent(name);
  }
}
