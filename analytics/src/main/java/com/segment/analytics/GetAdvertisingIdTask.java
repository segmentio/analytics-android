package com.segment.analytics;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;
import com.segment.analytics.integrations.Logger;
import java.util.concurrent.CountDownLatch;

/**
 * An {@link AsyncTask} that fetches the advertising info and attaches it to the given {@link
 * AnalyticsContext} instance.
 */
class GetAdvertisingIdTask extends AsyncTask<Context, Void, Pair<String, Boolean>> {

  final AnalyticsContext analyticsContext;
  final CountDownLatch latch;
  final Logger logger;

  GetAdvertisingIdTask(AnalyticsContext analyticsContext, CountDownLatch latch, Logger logger) {
    this.analyticsContext = analyticsContext;
    this.latch = latch;
    this.logger = logger;
  }

  @Override protected Pair<String, Boolean> doInBackground(Context... contexts) {
    final Context context = contexts[0];
    try {
      Object advertisingInfo =
          Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
              .getMethod("getAdvertisingIdInfo", Context.class)
              .invoke(null, context);
      Boolean isLimitAdTrackingEnabled = (Boolean) advertisingInfo.getClass()
          .getMethod("isLimitAdTrackingEnabled")
          .invoke(advertisingInfo);

      if (isLimitAdTrackingEnabled) {
        logger.debug("Not collecting advertising ID because isLimitAdTrackingEnabled is true.");
        return Pair.create(null, false);
      }

      String advertisingId =
          (String) advertisingInfo.getClass().getMethod("getId").invoke(advertisingInfo);
      return Pair.create(advertisingId, true);
    } catch (Exception e) {
      logger.error(e, "Unable to collect advertising ID.");
    }
    return null;
  }

  @Override protected void onPostExecute(Pair<String, Boolean> info) {
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
