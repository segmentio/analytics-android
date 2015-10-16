package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.content.Context;
import com.appsflyer.AppsFlyerLib;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.Log;
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
  Log log;

  // Used by reflection.
  @SuppressWarnings("unused") AppsFlyerIntegration() {
    this(AppsFlyer.DEFAULT);
  }

  AppsFlyerIntegration(AppsFlyer appsFlyer) {
    this.appsFlyer = appsFlyer;
  }

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    Context context = analytics.getApplication();
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new IllegalStateException("AppsFlyer requires the ACCESS_NETWORK_STATE permission");
    }
    this.context = context;
    log = analytics.getLogger().newLogger(APPS_FLYER_KEY);

    String appsFlyerDevKey = settings.getString("appsFlyerDevKey");
    appsFlyer.setAppsFlyerKey(appsFlyerDevKey);
    log.verbose("AppsFlyerLib.setAppsFlyerKey(%s);", appsFlyerDevKey);

    boolean httpFallback = settings.getBoolean("httpFallback", false);
    appsFlyer.setUseHTTPFallback(httpFallback);
    log.verbose("AppsFlyerLib.setUseHTTPFallback(%s);", httpFallback);
  }

  @Override public String key() {
    return APPS_FLYER_KEY;
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    String currency = track.properties().currency();
    if (!isNullOrEmpty(currency)) {
      appsFlyer.setCurrencyCode(currency);
      log.verbose("AppsFlyerLib.setCurrencyCode(%s);", currency);
    }
    String event = track.event();
    String value = String.valueOf(track.properties().value());
    appsFlyer.sendTrackingWithEvent(context, event, value);
    log.verbose("AppsFlyerLib.sendTrackingWithEvent(context, %s, %s);", event, value);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    appsFlyer.setAppUserId(userId);
    log.verbose("AppsFlyerLib.setAppUserId(%s);", userId);

    String email = identify.traits().email();
    appsFlyer.setUserEmail(email);
    log.verbose("AppsFlyerLib.setUserEmail(%s);", email);
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

      @Override public void setUseHTTPFallback(boolean useHttp) {
        AppsFlyerLib.setUseHTTPFalback(useHttp);
      }

      @Override public void setCurrencyCode(String currencyCode) {
        AppsFlyerLib.setCurrencyCode(currencyCode);
      }

      @Override
      public void sendTrackingWithEvent(Context context, String eventName, String eventValue) {
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

    void setUseHTTPFallback(boolean useHttp);

    void setCurrencyCode(String currencyCode);

    void sendTrackingWithEvent(Context context1, String eventName, String eventValue);

    void setAppUserId(String userId);

    void setUserEmail(String email);
  }
}
