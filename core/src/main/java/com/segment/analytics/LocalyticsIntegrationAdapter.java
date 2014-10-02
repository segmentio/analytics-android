package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import com.localytics.android.LocalyticsSession;
import java.util.Map;

import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * Localytics is a general-purpose mobile analytics tool that measures customer acquisition, ad
 * attribution, retargeting campaigns and user actions in your mobile apps.
 *
 * @see <a href="http://www.localytics.com/">Localytics</a>
 * @see <a href="https://segment.io/docs/integrations/localytics/">Localytics Integration</a>
 * @see <a href="http://www.localytics.com/docs/android-integration/">Localytics Android SDK</a>
 */
class LocalyticsIntegrationAdapter extends AbstractIntegrationAdapter<LocalyticsSession> {
  private LocalyticsSession localyticsSession;

  @Override void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    // todo: docs mentions wake_lock, but not if it is required
    localyticsSession = new LocalyticsSession(context, settings.getString("appKey"));
  }

  @Override LocalyticsSession getUnderlyingInstance() {
    return localyticsSession;
  }

  @Override String key() {
    return "Localytics";
  }

  @Override void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    localyticsSession.open();
  }

  @Override void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    localyticsSession.close();
  }

  @Override void flush() {
    super.flush();
    localyticsSession.upload();
  }

  @Override void optOut(boolean optOut) {
    super.optOut(optOut);
    localyticsSession.setOptOut(optOut);
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    localyticsSession.setCustomerId(identify.userId());
    Traits traits = identify.traits();
    String email = traits.email();
    if (!isNullOrEmpty(email)) localyticsSession.setCustomerEmail(email);
    String name = traits.name();
    if (!isNullOrEmpty(name)) localyticsSession.setCustomerName(name);
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      localyticsSession.setCustomerData(entry.getKey(), String.valueOf(entry.getValue()));
    }
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    localyticsSession.tagScreen(screen.event());
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    localyticsSession.tagEvent(track.event(), track.properties().toStringMap());
  }
}
