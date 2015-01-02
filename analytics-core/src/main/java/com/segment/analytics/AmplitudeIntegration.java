package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import com.amplitude.api.Amplitude;

import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * Amplitude is an event tracking and segmentation tool for your mobile apps. By analyzing the
 * actions your users perform you can gain a better understanding of how they use your app.
 *
 * @see <a href="https://amplitude.com">Amplitude</a>
 * @see <a href="https://Segment/docs/integrations/amplitude/">Amplitude Integration</a>
 * @see <a href="https://github.com/amplitude/Amplitude-Android">Amplitude Android SDK</a>
 */
class AmplitudeIntegration extends AbstractIntegration<Void> {
  static final String AMPLITUDE_KEY = "Amplitude";
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;

  @Override void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    trackAllPages = settings.getBoolean("trackAllPages", false);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", false);
    trackNamedPages = settings.getBoolean("trackNamedPages", false);
    Amplitude.initialize(context, settings.getString("apiKey"));
  }

  @Override Void getUnderlyingInstance() {
    return null;
  }

  @Override String key() {
    return AMPLITUDE_KEY;
  }

  @Override void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    Amplitude.startSession();
  }

  @Override void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    Amplitude.endSession();
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    Traits traits = identify.traits();
    Amplitude.setUserId(userId);
    Amplitude.setUserProperties(traits.toJsonObject());
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    if (trackAllPages) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    event(track.event(), track.properties());
  }

  private void event(String name, Properties properties) {
    Amplitude.logEvent(name, properties.toJsonObject());
    double revenue = properties.getDouble("revenue", -1);
    if (revenue != -1) {
      String productId = properties.getString("productId");
      int quantity = properties.getInt("quantity", 0);
      String receipt = properties.getString("receipt");
      String receiptSignature = properties.getString("receiptSignature");
      Amplitude.logRevenue(productId, quantity, revenue, receipt, receiptSignature);
    }
  }

  @Override void flush() {
    super.flush();
    Amplitude.uploadEvents();
  }
}
