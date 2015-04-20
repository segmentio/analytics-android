package com.segment.analytics.internal.integrations;

import android.content.Context;
import com.apptimize.Apptimize;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.Map.Entry;

import static com.segment.analytics.Analytics.LogLevel;

/**
 * Apptimize allows you to instantly update your native app without waiting for
 * App or Play Store approvals, and easily see if the change improved the app
 * with robust A/B testing analytics.
 *
 * @see <a href="http://www.apptimize.com/">Apptimize</a>
 * @see <a href="https://segment.com/docs/integrations/apptimize/">Apptimize Integration</a>
 */
public class ApptimizeIntegration extends AbstractIntegration<Void> {
  static final String APPTIMIZE_KEY = "Apptimize";

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    Apptimize.setup(context, settings.getString("appkey"));
  }

  @Override public String key() {
    return APPTIMIZE_KEY;
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    for (Entry<String, Object> entry : identify.traits().entrySet()) {
      if (entry.getValue() instanceof Integer) {
        Apptimize.setUserAttribute(entry.getKey(), (Integer) entry.getValue());
      } else {
        Apptimize.setUserAttribute(entry.getKey(), String.valueOf(entry.getValue()));
      }
    }
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    double value = track.properties().getDouble("value", Double.MIN_VALUE);
    if (value == Double.MIN_VALUE) {
      Apptimize.track(track.event());
    } else {
      Apptimize.track(track.event(), value);
    }
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    Apptimize.track(screen.event());
  }
}
