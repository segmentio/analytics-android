package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.quantcast.measurement.service.QuantcastClient;

import static com.segment.analytics.Utils.hasPermission;

/**
 * Quantcast is an audience measurement tool that captures demographic and traffic data about the
 * visitors to your site, to make sure your ads are targeted at the right people.
 *
 * @see <a href="https://www.quantcast.com/">Quantcast</a>
 * @see <a href="https://segment.io/docs/integrations/quantcast/">Quantcast Integration</a>
 * @see <a href="https://github.com/quantcast/android-measurement#quantcast-android-sdk">Quantcast
 * Android SDK</a>
 */
class QuantcastIntegrationAdapter extends AbstractIntegrationAdapter<Void> {
  String apiKey;

  QuantcastIntegrationAdapter(boolean debuggingEnabled) {
    super(debuggingEnabled);
  }

  @Override void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new InvalidConfigurationException("ACCESS_NETWORK_STATE is required");
    }
    apiKey = settings.getString("apiKey");
    // todo: set logger
  }

  @Override Void getUnderlyingInstance() {
    return null;
  }

  @Override String key() {
    return "Quantcast";
  }

  @Override void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    QuantcastClient.activityStart(activity, apiKey, null, null);
  }

  @Override void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    QuantcastClient.activityStop();
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    QuantcastClient.recordUserIdentifier(identify.userId());
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    QuantcastClient.logEvent(String.format(VIEWED_EVENT_FORMAT, screen.name()));
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    QuantcastClient.logEvent(track.event());
  }

  @Override void optOut(boolean optOut) {
    super.optOut(optOut);
    QuantcastClient.setCollectionEnabled(optOut);
  }
}
