package com.segment.analytics;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Pair;
import com.segment.analytics.integrations.Logger;
import java.util.concurrent.CountDownLatch;

/**
 * An {@link AsyncTask} that fetches the advertising info and attaches it to the given {@link
 * AnalyticsContext} instance.
 */
class GetAdvertisingIdTask extends AsyncTask<Context, Void, Pair<String, Boolean>> {

  private final AnalyticsContext analyticsContext;
  private final CountDownLatch latch;
  private final Logger logger;

  GetAdvertisingIdTask(AnalyticsContext analyticsContext, CountDownLatch latch, Logger logger) {
    this.analyticsContext = analyticsContext;
    this.latch = latch;
    this.logger = logger;
  }

  private Pair<String, Boolean> getGooglePlayServicesAdvertisingID(Context context)
      throws Exception {
    Object advertisingInfo =
        Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
            .getMethod("getAdvertisingIdInfo", Context.class)
            .invoke(null, context);
    Boolean isLimitAdTrackingEnabled =
        (Boolean)
            advertisingInfo
                .getClass()
                .getMethod("isLimitAdTrackingEnabled")
                .invoke(advertisingInfo);

    if (isLimitAdTrackingEnabled) {
      logger.debug(
          "Not collecting advertising ID because isLimitAdTrackingEnabled (Google Play Services) is true.");
      return Pair.create(null, false);
    }

    String advertisingId =
        (String) advertisingInfo.getClass().getMethod("getId").invoke(advertisingInfo);
    return Pair.create(advertisingId, true);
  }

  private Pair<String, Boolean> getAmazonFireAdvertisingID(Context context) throws Exception {
    ContentResolver contentResolver = context.getContentResolver();

    // Ref: http://prateeks.link/2uGs6bf
    // limit_ad_tracking != 0 indicates user wants to limit ad tracking.
    boolean limitAdTracking = Secure.getInt(contentResolver, "limit_ad_tracking") != 0;

    if (limitAdTracking) {
      logger.debug(
          "Not collecting advertising ID because limit_ad_tracking (Amazon Fire OS) is true.");
      return Pair.create(null, false);
    }

    String advertisingId = Secure.getString(contentResolver, "advertising_id");
    return Pair.create(advertisingId, true);
  }

  @Override
  protected Pair<String, Boolean> doInBackground(Context... contexts) {
    final Context context = contexts[0];
    try {
      return getGooglePlayServicesAdvertisingID(context);
    } catch (Exception e) {
      logger.error(e, "Unable to collect advertising ID from Google Play Services.");
    }
    try {
      return getAmazonFireAdvertisingID(context);
    } catch (Exception e) {
      logger.error(e, "Unable to collect advertising ID from Amazon Fire OS.");
    }
    logger.debug("Unable to collect advertising ID from Amazon Fire OS and Google Play Services.");
    return null;
  }

  @Override
  protected void onPostExecute(Pair<String, Boolean> info) {
    super.onPostExecute(info);

    try {
      if (info == null) {
        return;
      }
      AnalyticsContext.Device device = analyticsContext.device();
      if (device == null) {
        logger.debug("Not collecting advertising ID because context.device is null.");
        return;
      }
      device.putAdvertisingInfo(info.first, info.second);
    } finally {
      latch.countDown();
    }
  }
}
