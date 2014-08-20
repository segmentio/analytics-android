package com.segment.android.internal.integrations;

import android.content.Context;
import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.segment.android.Properties;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.settings.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.Map;

public class CrittercismIntegration extends AbstractIntegration<Void> {
  public CrittercismIntegration() throws ClassNotFoundException {
    super("Crittercism", "com.crittercism.app.Crittercism");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // no extra required permissions
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    CrittercismSettings settings = new CrittercismSettings(projectSettings.getJsonMap(key()));
    Crittercism.initialize(context, settings.appId(), settings.getConfig());
    return true;
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    Crittercism.setUsername(identify.userId());
    Traits traits = identify.getTraits();
    Crittercism.setMetadata(traits.toJsonObject()); // todo: have more than 10
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    event("Viewed " + screen.name() + " Screen", screen.properties());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    event(track.event(), track.properties());
  }

  private void event(String name, Properties properties) {
    Crittercism.leaveBreadcrumb(name);
  }

  @Override public void flush() {
    super.flush();
    Crittercism.sendAppLoadData();
  }

  @Override public void optOut(boolean optOut) {
    super.optOut(optOut);
    Crittercism.setOptOutStatus(optOut);
  }

  static class CrittercismSettings extends JsonMap {
    CrittercismSettings(Map<String, Object> delegate) {
      super(delegate);
    }

    String appId() {
      return getString("appId");
    }

    boolean includeVersionCode() {
      return getBoolean("includeVersionCode");
    }

    boolean shouldCollectLogcat() {
      return getBoolean("shouldCollectLogcat");
    }

    CrittercismConfig getConfig() {
      CrittercismConfig crittercismConfig = new CrittercismConfig();
      crittercismConfig.setLogcatReportingEnabled(shouldCollectLogcat());
      crittercismConfig.setVersionCodeToBeIncludedInVersionString(includeVersionCode());
      return crittercismConfig;
    }
  }
}
