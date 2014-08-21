package com.segment.android.internal.integrations;

import android.content.Context;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.android.Properties;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.Map;
import org.json.JSONObject;

public class MixpanelIntegration extends AbstractIntegration<MixpanelAPI> {
  MixpanelAPI mixpanelAPI;
  boolean isPeopleEnabled;

  public MixpanelIntegration() throws ClassNotFoundException {
    super("Mixpanel", "com.mixpanel.android.mpmetrics.MixpanelAPI");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // no extra permissions
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    MixpanelSettings settings = new MixpanelSettings(projectSettings.getJsonMap(key()));
    mixpanelAPI = MixpanelAPI.getInstance(context, settings.token());
    isPeopleEnabled = settings.people();
    return true;
  }

  @Override public MixpanelAPI getUnderlyingInstance() {
    return mixpanelAPI;
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    mixpanelAPI.identify(userId);
    JSONObject traits = identify.getTraits().toJsonObject();
    mixpanelAPI.registerSuperProperties(traits);
    if (isPeopleEnabled) {
      MixpanelAPI.People people = mixpanelAPI.getPeople();
      people.identify(userId);
      people.set(traits);
    }
  }

  @Override public void flush() {
    super.flush();
    mixpanelAPI.flush();
  }

  @Override public void alias(AliasPayload alias) {
    super.alias(alias);
    mixpanelAPI.alias(alias.userId(), alias.previousId());
  }

  @Override
  public void screen(ScreenPayload screen) {
    event("Viewed " + screen.name() + " Screen", screen.properties());
  }

  @Override
  public void track(TrackPayload track) {
    event(track.event(), track.properties());
  }

  private void event(String name, Properties properties) {
    JSONObject props = properties.toJsonObject();
    mixpanelAPI.track(name, props);
    if (isPeopleEnabled && properties.containsKey("revenue")) {
      MixpanelAPI.People people = mixpanelAPI.getPeople();
      double revenue = properties.getDouble("revenue");
      people.trackCharge(revenue, props);
    }
  }

  static class MixpanelSettings extends JsonMap {
    MixpanelSettings(Map<String, Object> delegate) {
      super(delegate);
    }

    boolean people() {
      return getBoolean("people");
    }

    String token() {
      return getString("token");
    }
  }
}
