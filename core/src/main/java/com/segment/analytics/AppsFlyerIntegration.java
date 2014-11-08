package com.segment.analytics;

import android.Manifest;
import android.content.Context;
import com.appsflyer.AppsFlyerLib;

import static com.segment.analytics.Utils.hasPermission;
import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * AppsFlyer is a mobile app measurement and tracking tool.
 *
 * @see <a href="http://www.appsflyer.com/">AppsFlyer</a>
 * @see <a href="https://segment.com/docs/integrations/appsflyer/">AppsFlyer Integration</a>
 * @see <a href="http://www.appsflyer.com/">AppsFlyer Website</a>
 */
class AppsFlyerIntegration extends AbstractIntegration<Void> {
  static final String APPS_FLYER_KEY = "AppsFlyer";
  Context context;

  @Override void initialize(Context context, JsonMap settings, boolean debuggingEnabled)
      throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new InvalidConfigurationException(
          "AppsFlyer requires the ACCESS_NETWORK_STATE permission");
    }
    AppsFlyerLib.setAppsFlyerKey(settings.getString("appsFlyerDevKey"));
    AppsFlyerLib.setUseHTTPFalback(settings.getBoolean("httpFallback", false));
    this.context = context;
  }

  @Override String key() {
    return APPS_FLYER_KEY;
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    String currency = track.properties().currency();
    if (!isNullOrEmpty(currency)) {
      AppsFlyerLib.setCurrencyCode(track.properties().currency());
    }
    AppsFlyerLib.sendTrackingWithEvent(context, track.event(),
        String.valueOf(track.properties().value()));
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    AppsFlyerLib.setAppUserId(identify.userId());
    AppsFlyerLib.setUserEmail(identify.traits().email());
  }
}
