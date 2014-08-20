package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.bugsnag.android.Bugsnag;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.settings.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.Iterator;
import java.util.Map;

import static com.segment.android.internal.Utils.isNullOrEmpty;

public class BugsnagIntegration extends AbstractIntegration {
  private static final String USER_KEY = "User";

  public BugsnagIntegration() throws ClassNotFoundException {
    super("Bugsnag", "com.bugsnag.android.Bugsnag");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    // No extra permissions
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    BugsnagSettings settings = new BugsnagSettings(projectSettings.getJsonMap(key()));
    String apiKey = settings.apiKey();
    if (isNullOrEmpty(apiKey)) {
      throw new InvalidConfigurationException("Bugsnag requires the apiKey setting.");
    }
    Bugsnag.setUseSSL(settings.useSSL());
    Bugsnag.register(context, apiKey);
    return false;
  }

  @Override public Object getUnderlyingInstance() {
    return null;
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    Bugsnag.setContext(activity.getLocalClassName());
    Bugsnag.onActivityCreate(activity);
  }

  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    Bugsnag.onActivityResume(activity);
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    Bugsnag.onActivityPause(activity);
  }

  @Override public void onActivityDestroyed(Activity activity) {
    super.onActivityDestroyed(activity);
    Bugsnag.onActivityDestroy(activity);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);

    Traits traits = identify.getTraits();

    Bugsnag.setUser(traits.id(), traits.email(), traits.name());

    Iterator<String> keys = traits.keySet().iterator();
    while (keys.hasNext()) {
      String key = keys.next();
      Object value = traits.get(key);
      Bugsnag.addToTab("User", key, value);
    }
  }

  static class BugsnagSettings extends JsonMap {
    BugsnagSettings(Map<String, Object> delegate) {
      super(delegate);
    }

    String apiKey() {
      return getString("apiKey");
    }

    boolean useSSL() {
      return containsKey("useSSL") ? getBoolean("useSSL") : false;
    }
  }
}
