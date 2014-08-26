package com.segment.android.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.quantcast.measurement.service.QuantcastClient;
import com.segment.android.Integration;
import com.segment.android.internal.Logger;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;

import static com.segment.android.internal.Utils.hasPermission;

/**
 * Quantcast is an audience measurement tool that captures demographic and traffic data about the
 * visitors to your site, to make sure your ads are targeted at the right people.
 *
 * @see {@link https://www.quantcast.com/}
 * @see {@link https://segment.io/docs/integrations/quantcast/}
 * @see {@link https://github.com/quantcast/android-measurement#quantcast-android-sdk}
 */
public class QuantcastIntegrationAdapter extends AbstractIntegrationAdapter<Void> {
  String apiKey;

  @Override public Integration provider() {
    return Integration.QUANTCAST;
  }

  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new InvalidConfigurationException("ACCESS_NETWORK_STATE is required");
    }
    apiKey = settings.getString("apiKey");
    QuantcastClient.enableLogging(Logger.isLogging());
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
    QuantcastClient.logEvent(String.format(VIEWED_EVENT_FORMAT, screen.name()));
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    QuantcastClient.logEvent(track.event());
  }

  @Override public void optOut(boolean optOut) {
    super.optOut(optOut);
    QuantcastClient.setCollectionEnabled(optOut);
  }
}
