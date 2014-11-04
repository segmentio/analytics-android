package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;

import static com.segment.analytics.Utils.hasPermission;

class LeanplumIntegration extends AbstractIntegration<Void> {
  LeanplumActivityHelper helper;

  LeanplumIntegration(boolean debuggingEnabled) {
    super(debuggingEnabled);
  }

  @Override void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new InvalidConfigurationException("Leanplum requires ACCESS_NETWORK_STATE permission");
    }
    if (!hasPermission(context, "com.google.android.c2dm.permission.RECEIVE")) {
      throw new InvalidConfigurationException(
          "Leanplum requires com.google.android.c2dm.permission.RECEIVE permission");
    }
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
    return "Leanplum";
  }
}
