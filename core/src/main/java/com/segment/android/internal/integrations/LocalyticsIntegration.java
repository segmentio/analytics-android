package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.localytics.android.LocalyticsSession;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.settings.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.Map;

import static com.segment.android.internal.Utils.isNullOrEmpty;

/**
 * @see http://www.localytics.com/docs/android-integration/
 */
public class LocalyticsIntegration extends AbstractIntegration<LocalyticsSession> {
  private LocalyticsSession localyticsSession;

  public LocalyticsIntegration() throws ClassNotFoundException {
    super("Localytics", "com.localytics.android.LocalyticsSession");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // no extra permissions
    // todo: docs mentions wake_lock, but not if it is required...
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    LocalyticsSettings settings = new LocalyticsSettings(projectSettings.getJsonMap(key()));
    localyticsSession = new LocalyticsSession(context, settings.appKey());
    return true;
  }

  @Override public LocalyticsSession getUnderlyingInstance() {
    return localyticsSession;
  }

  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    localyticsSession.open();
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    localyticsSession.close();
  }

  @Override public void flush() {
    super.flush();
    localyticsSession.upload();
  }

  @Override public void optOut(boolean optOut) {
    super.optOut(optOut);
    localyticsSession.setOptOut(optOut);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    localyticsSession.setCustomerId(identify.userId());
    Traits traits = identify.getTraits();
    String email = traits.email();
    if (!isNullOrEmpty(email)) localyticsSession.setCustomerEmail(email);
    String name = traits.name();
    if (!isNullOrEmpty(name)) localyticsSession.setCustomerName(name);
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      localyticsSession.setCustomerData(entry.getKey(), String.valueOf(entry.getValue()));
    }
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    localyticsSession.tagScreen(screen.name());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    localyticsSession.tagEvent(track.event(), track.properties().toStringMap());
  }

  static class LocalyticsSettings extends JsonMap {
    LocalyticsSettings(Map<String, Object> delegate) {
      super(delegate);
    }

    String appKey() {
      return getString("appKey");
    }
  }
}
