package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Client;
import com.segment.android.internal.Integration;
import com.segment.android.Traits;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.json.JsonMap;
import java.util.Map;

/**
 * Bugsnag is an error tracking service for websites and mobile apps. It automatically captures any
 * errors in your code so that you can find them and resolve them as quickly as possible.
 *
 * @see {@link https://bugsnag.com/}
 * @see {@link https://segment.io/docs/integrations/bugsnag/}
 * @see {@link https://github.com/bugsnag/bugsnag-android}
 */
public class BugsnagIntegrationAdapter extends AbstractIntegrationAdapter<Client> {

  @Override public Integration provider() {
    return Integration.BUGSNAG;
  }

  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    Bugsnag.register(context, settings.getString("apiKey"));
    Bugsnag.setUseSSL(settings.getBoolean("useSSL"));
  }

  @Override public Client getUnderlyingInstance() {
    return Bugsnag.getClient();
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
    Traits traits = identify.traits();
    Bugsnag.setUser(traits.userId(), traits.email(), traits.name());
    final String userKey = "User";
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      Bugsnag.addToTab(userKey, entry.getKey(), entry.getValue());
    }
  }
}
