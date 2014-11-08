package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.localytics.android.LocalyticsAmpSession;
import java.util.Map;

import static com.segment.analytics.Utils.hasPermission;
import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * Localytics is a general-purpose mobile analytics tool that measures customer acquisition, ad
 * attribution, retargeting campaigns and user actions in your mobile apps.
 *
 * @see <a href="http://www.localytics.com/">Localytics</a>
 * @see <a href="https://segment.io/docs/integrations/localytics/">Localytics Integration</a>
 * @see <a href="http://www.localytics.com/docs/android-integration/">Localytics Android SDK</a>
 */
class LocalyticsIntegration extends AbstractIntegration<LocalyticsAmpSession> {
  static final String LOCALYTICS_KEY = "Localytics";
  LocalyticsAmpSession session;

  @Override void initialize(Context context, JsonMap settings, boolean debuggingEnabled)
      throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.WAKE_LOCK)) {
      throw new InvalidConfigurationException("localytics requires the wake lock permission");
    }
    session = new LocalyticsAmpSession(context, settings.getString("appKey"));
  }

  @Override LocalyticsAmpSession getUnderlyingInstance() {
    return session;
  }

  @Override String key() {
    return LOCALYTICS_KEY;
  }

  @Override void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);
    session.open();
    session.upload();
  }

  @Override void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    session.open();
    session.upload();
    if (activity instanceof android.support.v4.app.FragmentActivity) {
      session.attach((android.support.v4.app.FragmentActivity) activity);
    }
  }

  @Override void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    if (activity instanceof android.support.v4.app.FragmentActivity) {
      session.detach();
    }
    session.close();
    session.upload();
  }

  @Override void flush() {
    super.flush();
    session.upload();
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    Traits traits = identify.traits();

    session.setCustomerId(identify.userId());

    String email = traits.email();
    if (!isNullOrEmpty(email)) session.setCustomerEmail(email);

    String name = traits.name();
    if (!isNullOrEmpty(name)) session.setCustomerName(name);

    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      session.setCustomerData(entry.getKey(), String.valueOf(entry.getValue()));
    }
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    session.tagScreen(screen.event());
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    session.tagEvent(track.event(), track.properties().toStringMap());
  }
}
