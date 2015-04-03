package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.internal.Utils.hasPermission;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Leanplum enables mobile teams to quickly go from insight to action using the lean cycle of
 * releasing, analyzing and optimizing content and messaging.
 *
 * @see <a href="http://www.leanplum.com/">Leanplum</a>
 * @see <a href="https://segment.com/docs/integrations/leanplum/">Leanplum Integration</a>
 */
public class LeanplumIntegration extends AbstractIntegration<Void> {
  static final String LEANPLUM_KEY = "Leanplum";
  LeanplumActivityHelper helper;

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new IllegalStateException("Leanplum requires ACCESS_NETWORK_STATE permission");
    }
    if (!hasPermission(context, "com.google.android.c2dm.permission.RECEIVE")) {
      throw new IllegalStateException(
          "Leanplum requires com.google.android.c2dm.permission.RECEIVE permission");
    }
    /*
    if (debuggingEnabled) {
      Leanplum.setAppIdForDevelopmentMode(settings.getString("appId"),
          settings.getString("clientKey"));
      Leanplum.enableVerboseLoggingInDevelopmentMode();
    }
    */

    boolean useLeanplumSenderId = settings.getBoolean("useLeanplumSenderId", false);
    String gcmSenderId = settings.getString("gcmSenderId");

    if (useLeanplumSenderId && !isNullOrEmpty(gcmSenderId)) {
      LeanplumPushService.setGcmSenderIds(gcmSenderId, LeanplumPushService.LEANPLUM_SENDER_ID);
    } else if (useLeanplumSenderId) {
      LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);
    } else if (!isNullOrEmpty(gcmSenderId)) {
      LeanplumPushService.setGcmSenderId(gcmSenderId);
    }

    Leanplum.setAppIdForProductionMode(settings.getString("appId"),
        settings.getString("clientKey"));
    Leanplum.start(context);
  }

  @Override public boolean track(TrackPayload track) {
    Leanplum.track(track.event(),
        track.properties().price() == 0 ? track.properties().price() : track.properties().value(),
        track.properties());
    return true;
  }

  @Override public boolean screen(ScreenPayload screen) {
    Leanplum.advanceTo(screen.name(), screen.category(), screen.properties());
    return true;
  }

  @Override public boolean identify(IdentifyPayload identify) {
    Leanplum.setUserAttributes(identify.userId(), identify.traits());
    return true;
  }

  @Override public boolean onActivityCreated(Activity activity, Bundle savedInstanceState) {
    helper = new LeanplumActivityHelper(activity);
    return true;
  }

  @Override public boolean onActivityResumed(Activity activity) {
    helper.onResume();
    return true;
  }

  @Override public boolean onActivityPaused(Activity activity) {
    helper.onPause();
    return true;
  }

  @Override public boolean onActivityStopped(Activity activity) {
    helper.onStop();
    return true;
  }

  @Override public String key() {
    return LEANPLUM_KEY;
  }
}
