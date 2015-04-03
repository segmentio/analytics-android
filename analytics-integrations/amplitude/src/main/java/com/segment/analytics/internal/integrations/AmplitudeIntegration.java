package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.amplitude.api.Amplitude;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Amplitude is an event tracking and segmentation tool for your mobile apps. By analyzing the
 * actions your users perform you can gain a better understanding of how they use your app.
 *
 * @see <a href="https://amplitude.com">Amplitude</a>
 * @see <a href="https://segment.com/docs/integrations/amplitude/">Amplitude Integration</a>
 * @see <a href="https://github.com/amplitude/Amplitude-Android">Amplitude Android SDK</a>
 */
public class AmplitudeIntegration extends AbstractIntegration<Void> {
  static final String AMPLITUDE_KEY = "Amplitude";
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    trackAllPages = settings.getBoolean("trackAllPages", false);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", false);
    trackNamedPages = settings.getBoolean("trackNamedPages", false);
    Amplitude.initialize(context, settings.getString("apiKey"));
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public String key() {
    return AMPLITUDE_KEY;
  }

  @Override public boolean onActivityResumed(Activity activity) {
    Amplitude.startSession();
    return true;
  }

  @Override public boolean onActivityPaused(Activity activity) {
    Amplitude.endSession();
    return true;
  }

  @Override public boolean identify(IdentifyPayload identify) {
    String userId = identify.userId();
    Traits traits = identify.traits();
    Amplitude.setUserId(userId);
    Amplitude.setUserProperties(traits.toJsonObject());
    return true;
  }

  @Override public boolean screen(ScreenPayload screen) {
    if (trackAllPages) {
      return event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      return event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      return event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
    return false;
  }

  @Override public boolean track(TrackPayload track) {
    return event(track.event(), track.properties());
  }

  private boolean event(String name, Properties properties) {
    Amplitude.logEvent(name, properties.toJsonObject());
    double revenue = properties.getDouble("revenue", -1);
    if (revenue != -1) {
      String productId = properties.getString("productId");
      int quantity = properties.getInt("quantity", 0);
      String receipt = properties.getString("receipt");
      String receiptSignature = properties.getString("receiptSignature");
      Amplitude.logRevenue(productId, quantity, revenue, receipt, receiptSignature);
    }
    return true;
  }

  @Override public boolean flush() {
    Amplitude.uploadEvents();
    return true;
  }
}
