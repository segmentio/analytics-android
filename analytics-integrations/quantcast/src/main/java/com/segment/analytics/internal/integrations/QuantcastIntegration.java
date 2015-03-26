package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.quantcast.measurement.service.QuantcastClient;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.internal.Utils.hasPermission;

/**
 * Quantcast is an audience measurement tool that captures demographic and traffic data about the
 * visitors to your site, to make sure your ads are targeted at the right people.
 *
 * @see <a href="https://www.quantcast.com/">Quantcast</a>
 * @see <a href="https://segment.com/docs/integrations/quantcast/">Quantcast Integration</a>
 * @see <a href="https://github.com/quantcast/android-measurement#quantcast-android-sdk">Quantcast
 * Android SDK</a>
 */
public class QuantcastIntegration extends AbstractIntegration<Void> {
  static final String QUANTCAST_KEY = "Quantcast";
  String apiKey;

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new IllegalStateException("ACCESS_NETWORK_STATE is required");
    }
    apiKey = settings.getString("apiKey");
    QuantcastClient.enableLogging(logLevel == INFO || logLevel == VERBOSE);
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public String key() {
    return QUANTCAST_KEY;
  }

  @Override public boolean onActivityStarted(Activity activity) {
    QuantcastClient.activityStart(activity, apiKey, null, null);
    return true;
  }

  @Override public boolean onActivityStopped(Activity activity) {
    QuantcastClient.activityStop();
    return true;
  }

  @Override public boolean identify(IdentifyPayload identify) {
    QuantcastClient.recordUserIdentifier(identify.userId());
    return true;
  }

  @Override public boolean screen(ScreenPayload screen) {
    QuantcastClient.logEvent(String.format(VIEWED_EVENT_FORMAT, screen.event()));
    return true;
  }

  @Override public boolean track(TrackPayload track) {
    QuantcastClient.logEvent(track.event());
    return true;
  }
}
