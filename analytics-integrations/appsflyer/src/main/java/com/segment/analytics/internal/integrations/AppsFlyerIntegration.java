package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.content.Context;
import com.appsflyer.AppsFlyerLib;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import static com.segment.analytics.internal.Utils.hasPermission;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * AppsFlyer is a mobile app measurement and tracking tool.
 *
 * @see <a href="http://www.appsflyer.com/">AppsFlyer</a>
 * @see <a href="https://segment.com/docs/integrations/appsflyer/">AppsFlyer Integration</a>
 * @see <a href="http://www.appsflyer.com/">AppsFlyer Website</a>
 */
public class AppsFlyerIntegration extends AbstractIntegration<Void> {
  static final String APPS_FLYER_KEY = "AppsFlyer";
  final AppsFlyer appsFlyer;
  Context context;

  AppsFlyerIntegration() {
    this(AppsFlyer.DEFAULT);
  }

  AppsFlyerIntegration(AppsFlyer appsFlyer) {
    this.appsFlyer = appsFlyer;
  }

  @Override public void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new IllegalStateException("AppsFlyer requires the ACCESS_NETWORK_STATE permission");
    }
    appsFlyer.setAppsFlyerKey(settings.getString("appsFlyerDevKey"));
    appsFlyer.setUseHTTPFallback(settings.getBoolean("httpFallback", false));
    this.context = context;
  }

  @Override public String key() {
    return APPS_FLYER_KEY;
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    String currency = track.properties().currency();
    if (!isNullOrEmpty(currency)) {
      appsFlyer.setCurrencyCode(track.properties().currency());
    }
    appsFlyer.sendTrackingWithEvent(context, track.event(),
        String.valueOf(track.properties().value()));
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    appsFlyer.setAppUserId(identify.userId());
    appsFlyer.setUserEmail(identify.traits().email());
  }

  /**
   * We can't mock AppsFlyerLib even with PowerMock, so we make a wrapper that can be tested.
   * <p></p> The relevant error which prevents the AppsFlyerLib class from being mocked
   * http://pastebin.com/jdZi9jPt
   */
  interface AppsFlyer {
    AppsFlyer DEFAULT = new AppsFlyer() {
      @Override public void setAppsFlyerKey(String key) {
        AppsFlyerLib.setAppsFlyerKey(key);
      }

      @Override public void setUseHTTPFallback(boolean isUseHttp) {
        AppsFlyerLib.setUseHTTPFalback(isUseHttp);
      }

      @Override public void setCurrencyCode(String currencyCode) {
        AppsFlyerLib.setCurrencyCode(currencyCode);
      }

      @Override public void sendTrackingWithEvent(Context context, String eventName,
          String eventValue) {
        AppsFlyerLib.sendTrackingWithEvent(context, eventName, eventValue);
      }

      @Override public void setAppUserId(String userId) {
        AppsFlyerLib.setAppUserId(userId);
      }

      @Override public void setUserEmail(String email) {
        AppsFlyerLib.setUserEmail(email);
      }
    };

    void setAppsFlyerKey(String key);

    void setUseHTTPFallback(boolean isUseHttp);

    void setCurrencyCode(String currencyCode);

    void sendTrackingWithEvent(Context context1, String eventName, String eventValue);

    void setAppUserId(String userId);

    void setUserEmail(String email);
  }
}
