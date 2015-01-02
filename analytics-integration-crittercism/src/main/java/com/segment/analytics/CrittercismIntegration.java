package com.segment.analytics;

import android.content.Context;
import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;

/**
 * Crittercism is an error reporting tool for your mobile apps. Any time your app crashes or
 * errors. Crittercism will collect logs that will help you debug the problem and fix your app.
 *
 * @see <a href="http://crittercism.com">Crittercism</a>
 * @see <a href="https://Segment/docs/integrations/crittercism">Crittercism Integration</a>
 * @see <a href="http://docs.crittercism.com/android/android.html">Crittercism Android SDK</a>
 */
public class CrittercismIntegration extends AbstractIntegration<Void> {
  static final String CRITTERCISM_KEY = "Crittercism";

  @Override void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    CrittercismConfig crittercismConfig = new CrittercismConfig();
    crittercismConfig.setLogcatReportingEnabled(settings.getBoolean("shouldCollectLogcat", false));
    crittercismConfig.setVersionCodeToBeIncludedInVersionString(
        settings.getBoolean("includeVersionCode", false));
    Crittercism.initialize(context, settings.getString("appId"), crittercismConfig);
  }

  @Override Void getUnderlyingInstance() {
    return null;
  }

  @Override String key() {
    return CRITTERCISM_KEY;
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    Crittercism.setUsername(identify.userId());
    Crittercism.setMetadata(identify.traits().toJsonObject());
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    Crittercism.leaveBreadcrumb(String.format(VIEWED_EVENT_FORMAT, screen.event()));
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    Crittercism.leaveBreadcrumb(track.event());
  }

  @Override void flush() {
    super.flush();
    Crittercism.sendAppLoadData();
  }
}
