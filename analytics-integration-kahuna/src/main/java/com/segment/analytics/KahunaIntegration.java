package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import com.kahuna.sdk.KahunaAnalytics;

import static com.kahuna.sdk.KahunaUserCredentialKeys.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USERNAME_KEY;
import static com.segment.analytics.Utils.isNullOrEmpty;
import static com.segment.analytics.Utils.isOnClassPath;

/**
 * Kahuna helps mobile marketers send push notifications and in-app messages.
 *
 * @see <a href="https://www.kahuna.com/">Kahuna</a>
 * @see <a href="https://segment.com/docs/integrations/kahuna/">Kahuna Integration</a>
 * @see <a href="http://app.usekahuna.com/tap/getstarted/android/">Kahuna Android SDK</a>
 */
public class KahunaIntegration extends AbstractIntegration<Void> {
  static final String KAHUNA_KEY = "Kahuna";

  @Override void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    if (!isOnClassPath("android.support.v4.app.Fragment")) {
      throw new IllegalStateException("Kahuna requires the support library to be bundled.");
    }
    KahunaAnalytics.onAppCreate(context, settings.getString("secretKey"),
        settings.getString("pushSenderId"));
    KahunaAnalytics.setDebugMode(debuggingEnabled);
  }

  @Override String key() {
    return KAHUNA_KEY;
  }

  @Override void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    KahunaAnalytics.start();
  }

  @Override void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    KahunaAnalytics.stop();
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);

    String username = identify.traits().username();
    KahunaAnalytics.setUsernameAndEmail(isNullOrEmpty(username) ? identify.userId() : username,
        identify.traits().email());

    KahunaAnalytics.setUserCredential(USERNAME_KEY, identify.traits().username());
    KahunaAnalytics.setUserCredential(EMAIL_KEY, identify.traits().email());
    KahunaAnalytics.setUserCredential(FACEBOOK_KEY, identify.traits().getString("facebook"));
    KahunaAnalytics.setUserCredential(TWITTER_KEY, identify.traits().getString("twitter"));
    KahunaAnalytics.setUserCredential(LINKEDIN_KEY, identify.traits().getString("linkedin"));

    KahunaAnalytics.setUserAttributes(identify.traits().toStringMap());
  }

  @Override void track(TrackPayload track) {
    super.track(track);

    KahunaAnalytics.trackEvent(track.event(), track.properties().getInt("count", 0),
        (int) track.properties().value());
  }
}
