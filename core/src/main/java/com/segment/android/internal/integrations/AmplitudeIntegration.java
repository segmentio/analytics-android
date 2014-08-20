package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.amplitude.api.Amplitude;
import com.segment.android.Properties;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.settings.AmplitudeSettings;
import com.segment.android.internal.settings.ProjectSettings;

import static com.segment.android.internal.Utils.isNullOrEmpty;

public class AmplitudeIntegration extends AbstractIntegration<Void> {
  private static final String REVENUE_KEY = "revenue";
  private static final String PRODUCT_ID_KEY = "productId";
  private static final String QUANTITY_KEY = "quantity";
  private static final String RECEIPT_KEY = "receipt";
  private static final String RECEIPT_SIGNATURE_KEY = "receiptSignature";

  /**
   * Create an integration with the given settings. Check for any specific permissions or features
   * that the integration needs. Also check for any required values in your settings.
   */
  public AmplitudeIntegration() throws ClassNotFoundException {
    super("Amplitude", "com.amplitude.api.Amplitude");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // only needs internet permission
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    AmplitudeSettings settings = new AmplitudeSettings(projectSettings.getJsonMap(key()));
    String apiKey = settings.apiKey();
    if (isNullOrEmpty(apiKey)) {
      throw new InvalidConfigurationException("Amplitude requires the apiKey setting.");
    }
    Amplitude.initialize(context, apiKey);
    return true;
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
    String userId = identify.getUserId();
    Traits traits = identify.getTraits();
    Amplitude.setUserId(userId);
    Amplitude.setUserProperties(traits.toJsonObject());
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    event("Viewed " + screen.getName() + " Screen", screen.getProperties());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    event(track.getEvent(), track.getProperties());
  }

  private void event(String name, Properties properties) {
    Amplitude.logEvent(name, properties.toJsonObject());

    if (properties.containsKey(REVENUE_KEY)) {
      double revenue = properties.getDouble(REVENUE_KEY);
      String productId = properties.getString(PRODUCT_ID_KEY);
      int quantity = properties.getInteger(QUANTITY_KEY);
      String receipt = properties.getString(RECEIPT_KEY);
      String receiptSignature = properties.getString(RECEIPT_SIGNATURE_KEY);
      Amplitude.logRevenue(productId, quantity, revenue, receipt, receiptSignature);
    }
  }

  @Override public void flush() {
    super.flush();
    Amplitude.uploadEvents();
  }
}
