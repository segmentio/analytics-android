package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;

import static com.segment.analytics.Utils.hasPermission;

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

  @Override void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
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
    Leanplum.setAppIdForProductionMode(settings.getString("appId"),
        settings.getString("clientKey"));
    Leanplum.start(context);
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    Leanplum.track(track.event(),
        track.properties().price() == 0 ? track.properties().price() : track.properties().value(),
        track.properties());
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    Leanplum.advanceTo(screen.name(), screen.category(), screen.properties());
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    Leanplum.setUserAttributes(identify.userId(), identify.traits());
  }

  @Override void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);
    helper = new LeanplumActivityHelper(activity);
  }

  @Override void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    helper.onResume();
  }

  @Override void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    helper.onPause();
  }

  @Override void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    helper.onStop();
  }

  @Override String key() {
    return LEANPLUM_KEY;
  }
}
