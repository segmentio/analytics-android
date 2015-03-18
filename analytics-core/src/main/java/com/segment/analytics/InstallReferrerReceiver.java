package com.segment.analytics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A {@link BroadcastReceiver} for automatically attaching Google Play Store referrer information
 * to the {@link AnalyticsContext}.
 * <p/>
 * Clients may subclass this and override {@link #getAnalytics(Context)} to provide custom
 * instances of {@link Analytics} client. This should be the same instance used to track events
 * through the rest of your app.
 */
public class InstallReferrerReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    getAnalytics(context).setInstallReferrer(context, intent);
  }

  protected Analytics getAnalytics(Context context) {
    return Analytics.with(context);
  }
}
