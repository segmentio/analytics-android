package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.amplitude.api.Amplitude;
import com.segment.android.Integration;
import com.segment.android.Properties;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;

import static com.segment.android.internal.Utils.isNullOrEmpty;
import static com.segment.android.internal.Utils.nullOrDefault;

/**
 * Amplitude is an event tracking and segmentation tool for your mobile apps. By analyzing the
 * actions your users perform you can gain a better understanding of how they use your app.
 *
 * @see {@link https://amplitude.com}
 * @see {@link https://segment.io/docs/integrations/amplitude/}
 * @see {@link https://github.com/amplitude/Amplitude-Android}
 */
public class AmplitudeIntegration extends AbstractIntegration<Void> {
  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;

  @Override public Integration provider() {
    return Integration.AMPLITUDE;
  }

  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    trackAllPages = settings.getBoolean("trackAllPages");
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages");
    trackNamedPages = settings.getBoolean("trackNamedPages");
    Amplitude.initialize(context, settings.getString("apiKey"));
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    Amplitude.startSession();
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    Amplitude.endSession();
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    Traits traits = identify.traits();
    Amplitude.setUserId(userId);
    Amplitude.setUserProperties(traits.toJsonObject());
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    if (trackAllPages) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    event(track.event(), track.properties());
  }

  private void event(String name, Properties properties) {
    Amplitude.logEvent(name, properties.toJsonObject());
    Double revenue = properties.getDouble("revenue");
    if (revenue != null) {
      String productId = properties.getString("productId");
      Integer quantity = properties.getInteger("quantity");
      String receipt = properties.getString("receipt");
      String receiptSignature = properties.getString("receiptSignature");
      Amplitude.logRevenue(productId, nullOrDefault(quantity, 0), revenue, receipt,
          receiptSignature);
    }
  }

  @Override public void flush() {
    super.flush();
    Amplitude.uploadEvents();
  }
}
