package com.segment.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.amplitude.api.Amplitude;

import static com.segment.android.Utils.isNullOrEmpty;

class AmplitudeIntegration extends Integration {
  private static final String AMPLITUDE_KEY = "Amplitude";
  private static final String API_KEY = "apiKey";

  private static final String REVENUE_KEY = "revenue";
  private static final String PRODUCT_ID_KEY = "productId";
  private static final String QUANTITY_KEY = "quantity";
  private static final String RECEIPT_KEY = "receipt";
  private static final String RECEIPT_SIGNATURE_KEY = "receiptSignature";

  @Override String getKey() {
    return AMPLITUDE_KEY;
  }

  @Override public void initialize(Context context, Json settings)
      throws InvalidConfigurationException {
    String apiKey = (String) settings.get(API_KEY);
    if (isNullOrEmpty(apiKey)) {
      throw new InvalidConfigurationException("Amplitude requires the apiKey setting.");
    }
    Amplitude.initialize(context, apiKey);
  }

  @Override public void onActivityStarted(Activity activity) {
    Amplitude.startSession();
  }

  @Override public void track(TrackPayload track) {
    event(track.getEvent(), track.getProperties());
  }

  @Override public void screen(ScreenPayload screen) {
    event("Viewed " + screen.getName() + " Screen", screen.getProperties());
  }

  private void event(String name, Properties properties) {
    Amplitude.logEvent(name, properties.toJsonObject());

    if (properties.has(REVENUE_KEY)) {
      double revenue = properties.getDouble(REVENUE_KEY);
      String productId = properties.getString(PRODUCT_ID_KEY);
      int quantity = properties.getInt(QUANTITY_KEY);
      String receipt = properties.getString(RECEIPT_KEY);
      String receiptSignature = properties.getString(RECEIPT_SIGNATURE_KEY);
      Amplitude.logRevenue(productId, quantity, revenue, receipt, receiptSignature);
    }
  }

  @Override public void identify(IdentifyPayload identify) {
    String userId = identify.getUserId();
    Traits traits = identify.getTraits();

    Amplitude.setUserId(userId);
    Amplitude.setUserProperties(traits.toJsonObject());
  }

  @Override public void onActivityStopped(Activity activity) {
    Amplitude.endSession();
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    // Ignore
  }

  @Override public void onActivityResumed(Activity activity) {
    // Ignore
  }

  @Override public void onActivityPaused(Activity activity) {
    // Ignore
  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    // Ignore
  }

  @Override public void onActivityDestroyed(Activity activity) {
    // Ignore
  }

  @Override public void group(GroupPayload group) {
    // Ignore
  }

  @Override public void alias(AliasPayload alias) {
    // Ignore
  }

  @Override public void reset() {
    // Ignore
  }

  @Override public void optOut(boolean optedOut) {
    // Ignore
  }

  @Override public void flush() {
    // Ignore
  }
}
