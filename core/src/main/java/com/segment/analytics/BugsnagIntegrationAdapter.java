package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Client;
import java.util.Map;

/**
 * Bugsnag is an error tracking service for websites and mobile apps. It automatically captures any
 * errors in your code so that you can find them and resolve them as quickly as possible.
 *
 * @see <a href="https://bugsnag.com/">Bugsnag</a>
 * @see <a href="https://segment.io/docs/integrations/bugsnag/">Bugsnag Integration</a>
 * @see <a href="https://github.com/bugsnag/bugsnag-android">Bugsnag Android SDK</a>
 */
class BugsnagIntegrationAdapter extends AbstractIntegrationAdapter<Client> {

  @Override void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    Bugsnag.register(context, settings.getString("apiKey"));
    Bugsnag.setUseSSL(settings.getBoolean("useSSL"));
  }

  @Override Client getUnderlyingInstance() {
    return Bugsnag.getClient();
  }

  @Override String className() {
    return "com.bugsnag.android.Bugsnag";
  }

  @Override String key() {
    return "Bugsnag";
  }

  @Override void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    Bugsnag.setContext(activity.getLocalClassName());
    Bugsnag.onActivityCreate(activity);
  }

  @Override void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    Bugsnag.onActivityResume(activity);
  }

  @Override void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    Bugsnag.onActivityPause(activity);
  }

  @Override void onActivityDestroyed(Activity activity) {
    super.onActivityDestroyed(activity);
    Bugsnag.onActivityDestroy(activity);
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    Traits traits = identify.traits();
    Bugsnag.setUser(traits.userId(), traits.email(), traits.name());
    final String userKey = "User";
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      Bugsnag.addToTab(userKey, entry.getKey(), entry.getValue());
    }
  }
}
