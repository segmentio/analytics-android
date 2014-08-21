package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.amplitude.api.Amplitude;
import com.segment.android.Properties;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.Map;

public class AmplitudeIntegration extends AbstractIntegration<Void> {

  public AmplitudeIntegration() throws ClassNotFoundException {
    super("Amplitude", "com.amplitude.api.Amplitude");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // no extra permissions
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    AmplitudeSettings settings = new AmplitudeSettings(projectSettings.getJsonMap(key()));
    Amplitude.initialize(context, settings.apiKey());
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
    String userId = identify.userId();
    Traits traits = identify.getTraits();
    Amplitude.setUserId(userId);
    Amplitude.setUserProperties(traits.toJsonObject());
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    event("Viewed " + screen.name() + " Screen", screen.properties());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    event(track.event(), track.properties());
  }

  private void event(String name, Properties properties) {
    Amplitude.logEvent(name, properties.toJsonObject());

    if (properties.containsKey("revenue")) {
      double revenue = properties.getDouble("revenue");
      String productId = properties.getString("productId");
      int quantity = properties.getInteger("quantity");
      String receipt = properties.getString("receipt");
      String receiptSignature = properties.getString("receiptSignature");
      Amplitude.logRevenue(productId, quantity, revenue, receipt, receiptSignature);
    }
  }

  @Override public void flush() {
    super.flush();
    Amplitude.uploadEvents();
  }

  static class AmplitudeSettings extends JsonMap {
    AmplitudeSettings(Map<String, Object> delegate) {
      super(delegate);
    }

    String apiKey() {
      return getString("apiKey");
    }

    boolean trackAllPages() {
      return containsKey("trackAllPages") ? getBoolean("trackAllPages") : false;
    }

    boolean trackCategorizedPages() {
      return containsKey("trackAllPages") ? getBoolean("trackAllPages") : false;
    }

    boolean trackNamedPages() {
      return containsKey("trackAllPages") ? getBoolean("trackAllPages") : false;
    }
  }
}
