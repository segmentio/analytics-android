package com.segment.analytics.internal.integrations;

import android.content.Context;
import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import static com.segment.analytics.Analytics.LogLevel;

/**
 * Crittercism is an error reporting tool for your mobile apps. Any time your app crashes or
 * errors.
 * Crittercism will collect logs that will help you debug the problem and fix your app.
 *
 * @see <a href="http://crittercism.com">Crittercism</a>
 * @see <a href="https://segment.com/docs/integrations/crittercism">Crittercism Integration</a>
 * @see <a href="http://docs.crittercism.com/android/android.html">Crittercism Android SDK</a>
 */
public class CrittercismIntegration extends AbstractIntegration<Void> {
  static final String CRITTERCISM_KEY = "Crittercism";

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    CrittercismConfig crittercismConfig = new CrittercismConfig();
    crittercismConfig.setLogcatReportingEnabled(settings.getBoolean("shouldCollectLogcat", false));
    crittercismConfig.setVersionCodeToBeIncludedInVersionString(
        settings.getBoolean("includeVersionCode", false));
    Crittercism.initialize(context, settings.getString("appId"), crittercismConfig);
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public String key() {
    return CRITTERCISM_KEY;
  }

  @Override public boolean identify(IdentifyPayload identify) {
    Crittercism.setUsername(identify.userId());
    Crittercism.setMetadata(identify.traits().toJsonObject());
    return true;
  }

  @Override public boolean screen(ScreenPayload screen) {
    Crittercism.leaveBreadcrumb(String.format(VIEWED_EVENT_FORMAT, screen.event()));
    return true;
  }

  @Override public boolean track(TrackPayload track) {
    Crittercism.leaveBreadcrumb(track.event());
    return true;
  }

  @Override public boolean flush() {
    Crittercism.sendAppLoadData();
    return true;
  }
}
