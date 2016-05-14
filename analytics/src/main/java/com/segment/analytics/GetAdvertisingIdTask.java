package com.segment.analytics;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

/**
 * An {@link AsyncTask} that fetches the advertising info and attaches it to the given {@link
 * AnalyticsContext} instance.
 */
class GetAdvertisingIdTask extends AsyncTask<Context, Void, Pair<String, Boolean>> {

  final AnalyticsContext analyticsContext;

  GetAdvertisingIdTask(AnalyticsContext analyticsContext) {
    this.analyticsContext = analyticsContext;
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
        return Pair.create(null, false);
      }

      String advertisingId =
          (String) advertisingInfo.getClass().getMethod("getId").invoke(advertisingInfo);
      return Pair.create(advertisingId, true);
    } catch (Exception ignored) {
      return null;
    }
  }

  @Override protected void onPostExecute(Pair<String, Boolean> info) {
    super.onPostExecute(info);
    if (info == null) {
      return;
    }

    AnalyticsContext.Device device = analyticsContext.device();
    if (device != null) {
      device.putAdvertisingInfo(info.first, info.second);
    }
  }
}
