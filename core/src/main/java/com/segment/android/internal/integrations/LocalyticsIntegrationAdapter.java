package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.localytics.android.LocalyticsSession;
import com.segment.android.Integration;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;
import java.util.Map;

import static com.segment.android.internal.Utils.isNullOrEmpty;

/**
 * Localytics is a general-purpose mobile analytics tool that measures customer acquisition, ad
 * attribution, retargeting campaigns and user actions in your mobile apps.
 *
 * @see {@link http://www.localytics.com/}
 * @see {@link https://segment.io/docs/integrations/localytics/}
 * @see {@link http://www.localytics.com/docs/android-integration/}
 */
public class LocalyticsIntegrationAdapter extends AbstractIntegrationAdapter<LocalyticsSession> {
  private LocalyticsSession localyticsSession;

  @Override public Integration provider() {
    return Integration.LOCALYTICS;
  }

  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    // todo: docs mentions wake_lock, but not if it is required
    localyticsSession = new LocalyticsSession(context, settings.getString("appKey"));
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
    Traits traits = identify.traits();
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
    localyticsSession.tagScreen(screen.event());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    localyticsSession.tagEvent(track.event(), track.properties().toStringMap());
  }
}
