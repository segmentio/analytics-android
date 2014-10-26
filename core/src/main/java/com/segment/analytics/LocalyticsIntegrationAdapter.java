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
  LocalyticsSession localyticsSession;

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
    getUnderlyingInstance().open();
  }

  @Override void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    getUnderlyingInstance().close();
  }

  @Override void flush() {
    super.flush();
    getUnderlyingInstance().upload();
  }

  @Override void optOut(boolean optOut) {
    super.optOut(optOut);
    getUnderlyingInstance().setOptOut(optOut);
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    getUnderlyingInstance().setCustomerId(identify.userId());
    Traits traits = identify.traits();
    String email = traits.email();
    if (!isNullOrEmpty(email)) getUnderlyingInstance().setCustomerEmail(email);
    String name = traits.name();
    if (!isNullOrEmpty(name)) getUnderlyingInstance().setCustomerName(name);
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      getUnderlyingInstance().setCustomerData(entry.getKey(), String.valueOf(entry.getValue()));
    }
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    getUnderlyingInstance().tagScreen(screen.event());
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    getUnderlyingInstance().tagEvent(track.event(), track.properties().toStringMap());
  }
}
