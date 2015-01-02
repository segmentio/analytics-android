package com.segment.analytics;

import android.content.Context;
import android.os.AsyncTask;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

class GetAdvertisingIdTask extends AsyncTask<Context, Void, AdvertisingIdClient.Info> {
  final AnalyticsContext analyticsContext;

  GetAdvertisingIdTask(AnalyticsContext analyticsContext) {
    this.analyticsContext = analyticsContext;
  }

  @Override protected AdvertisingIdClient.Info doInBackground(Context... contexts) {
    try {
      return AdvertisingIdClient.getAdvertisingIdInfo(contexts[0]);
    } catch (Exception ignored) {
      return null;
    }
  }

  @Override protected void onPostExecute(AdvertisingIdClient.Info info) {
    super.onPostExecute(info);
    if (info != null) {
      analyticsContext.putAdvertisingInfo(info.getId(), info.isLimitAdTrackingEnabled());
    }
  }
}
