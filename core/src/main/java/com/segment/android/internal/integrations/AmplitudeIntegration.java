package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.amplitude.api.Amplitude;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.util.Logger;
import com.segment.android.json.JsonMap;

import static com.segment.android.internal.util.Utils.isNullOrEmpty;

public class AmplitudeIntegration extends Integration {
  private static final String AMPLITUDE_KEY = "Amplitude";
  private static final String API_KEY = "apiKey";

  private static final String REVENUE_KEY = "revenue";
  private static final String PRODUCT_ID_KEY = "productId";
  private static final String QUANTITY_KEY = "quantity";
  private static final String RECEIPT_KEY = "receipt";
  private static final String RECEIPT_SIGNATURE_KEY = "receiptSignature";

  /**
   * Create an integration with the given settings. Check for any specific permissions or features
   * that the integration needs. Also check for any required values in your settings.
   */
  public AmplitudeIntegration(Context context) {
    super(context, AMPLITUDE_KEY);
  }

  @Override public void initialize(JsonMap integrationSettings)
      throws InvalidConfigurationException {
    String apiKey = (String) integrationSettings.get(API_KEY);
    if (isNullOrEmpty(apiKey)) {
      Logger.e("Amplitude requires the apiKey setting");
      disable();
    }
    Amplitude.initialize(getContext(), apiKey);
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

  }

  @Override public void onActivityStarted(Activity activity) {

  }

  @Override public void onActivityResumed(Activity activity) {

  }

  @Override public void onActivityPaused(Activity activity) {

  }

  @Override public void onActivityStopped(Activity activity) {

  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

  }

  @Override public void onActivityDestroyed(Activity activity) {

  }

  @Override public void identify(IdentifyPayload identify) {

  }

  @Override public void group(GroupPayload group) {

  }

  @Override public void track(TrackPayload track) {

  }

  @Override public void alias(AliasPayload alias) {

  }

  @Override public void screen(ScreenPayload screen) {

  }
}
