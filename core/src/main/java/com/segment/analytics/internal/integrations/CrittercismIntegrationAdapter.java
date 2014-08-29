package com.segment.analytics.internal.integrations;

import android.content.Context;
import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.segment.analytics.internal.payload.IdentifyPayload;
import com.segment.analytics.internal.payload.ScreenPayload;
import com.segment.analytics.internal.payload.TrackPayload;
import com.segment.analytics.json.JsonMap;

/**
 * Crittercism is an error reporting tool for your mobile apps. Any time your app crashes or
 * errors. Crittercism will collect logs that will help you debug the problem and fix your app.
 *
 * @see {@link http://crittercism.com}
 * @see {@link https://segment.io/docs/integrations/crittercism}
 * @see {@link http://docs.crittercism.com/android/android.html}
 */
public class CrittercismIntegrationAdapter extends AbstractIntegrationAdapter<Void> {
  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    CrittercismConfig crittercismConfig = new CrittercismConfig();
    crittercismConfig.setLogcatReportingEnabled(settings.getBoolean("shouldCollectLogcat"));
    crittercismConfig.setVersionCodeToBeIncludedInVersionString(
        settings.getBoolean("includeVersionCode"));
    Crittercism.initialize(context, settings.getString("appId"), crittercismConfig);
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public String className() {
    return "com.crittercism.app.Crittercism";
  }

  @Override public String key() {
    return "Crittercism";
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    Crittercism.setUsername(identify.userId());
    Crittercism.setMetadata(identify.traits().toJsonObject());
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    Crittercism.leaveBreadcrumb(String.format(VIEWED_EVENT_FORMAT, screen.event()));
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    Crittercism.leaveBreadcrumb(track.event());
  }

  @Override public void flush() {
    super.flush();
    Crittercism.sendAppLoadData();
  }

  @Override public void optOut(boolean optOut) {
    super.optOut(optOut);
    Crittercism.setOptOutStatus(optOut);
  }
}
