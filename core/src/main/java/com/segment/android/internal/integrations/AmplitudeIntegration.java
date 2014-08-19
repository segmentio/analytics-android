package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.amplitude.api.Amplitude;
import com.segment.android.Properties;
import com.segment.android.internal.ProjectSettings;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;

import static com.segment.android.internal.Utils.isNullOrEmpty;

public class AmplitudeIntegration
    extends AbstractIntegration<ProjectSettings.AmplitudeSettings, Void> {
  private static final String REVENUE_KEY = "revenue";
  private static final String PRODUCT_ID_KEY = "productId";
  private static final String QUANTITY_KEY = "quantity";
  private static final String RECEIPT_KEY = "receipt";
  private static final String RECEIPT_SIGNATURE_KEY = "receiptSignature";

  /**
   * Create an integration with the given settings. Check for any specific permissions or features
   * that the integration needs. Also check for any required values in your settings.
   */
  public AmplitudeIntegration(Context context, ProjectSettings.AmplitudeSettings settings)
      throws InvalidConfigurationException {
    super("Amplitude", context, settings);
    if (isNullOrEmpty(settings.apiKey)) {
      throw new InvalidConfigurationException("Amplitude requires the apiKey setting.");
    }

    Amplitude.initialize(context, settings.apiKey);
  }

  @Override public Void getUnderlyingInstance() {
    return null;
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
    Properties properties = track.getProperties();
    Amplitude.logEvent(track.getEvent(), properties.toJsonObject());
    // todo : commerce stuff
  }

  @Override public void alias(AliasPayload alias) {

  }

  @Override public void screen(ScreenPayload screen) {

  }
}
