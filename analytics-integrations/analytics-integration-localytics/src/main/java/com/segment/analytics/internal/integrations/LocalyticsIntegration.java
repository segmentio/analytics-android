package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.localytics.android.LocalyticsAmpSession;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.Map;

import static com.segment.analytics.internal.Utils.hasPermission;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.isOnClassPath;

/**
 * Localytics is a general-purpose mobile analytics tool that measures customer acquisition, ad
 * attribution, retargeting campaigns and user actions in your mobile apps.
 *
 * @see <a href="http://www.localytics.com/">Localytics</a>
 * @see <a href="https://segment.com/docs/integrations/localytics/">Localytics Integration</a>
 * @see <a href="http://www.localytics.com/docs/android-integration/">Localytics Android SDK</a>
 */
public class LocalyticsIntegration extends AbstractIntegration<LocalyticsAmpSession> {
  static final String LOCALYTICS_KEY = "Localytics";
  LocalyticsAmpSession session;
  boolean hasSupportLibOnClassPath;

  @Override public void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    if (!hasPermission(context, Manifest.permission.WAKE_LOCK)) {
      throw new IllegalStateException("localytics requires the wake lock permission");
    }
    session = new LocalyticsAmpSession(context, settings.getString("appKey"));
    LocalyticsAmpSession.setLoggingEnabled(debuggingEnabled);
    hasSupportLibOnClassPath = isOnClassPath("android.support.v4.app.FragmentActivity");
  }

  @Override public LocalyticsAmpSession getUnderlyingInstance() {
    return session;
  }

  @Override public String key() {
    return LOCALYTICS_KEY;
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);
    session.open();
    session.upload();
  }

  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    session.open();
    session.upload();

    if (hasSupportLibOnClassPath) {
      if (activity instanceof android.support.v4.app.FragmentActivity) {
        session.attach((android.support.v4.app.FragmentActivity) activity);
      }
    }
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    if (hasSupportLibOnClassPath) {
      if (activity instanceof android.support.v4.app.FragmentActivity) {
        session.detach();
      }
    }
    session.close();
    session.upload();
  }

  @Override public void flush() {
    super.flush();
    session.upload();
  }

  @Override public void identify(IdentifyPayload identify) {
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

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    session.tagScreen(screen.event());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    session.tagEvent(track.event(), track.properties().toStringMap());
  }
}
