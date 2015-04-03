package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.kahuna.sdk.KahunaAnalytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import static com.kahuna.sdk.KahunaUserCredentialKeys.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USERNAME_KEY;
import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.isOnClassPath;

/**
 * Kahuna helps mobile marketers send push notifications and in-app messages.
 *
 * @see <a href="https://www.kahuna.com/">Kahuna</a>
 * @see <a href="https://segment.com/docs/integrations/kahuna/">Kahuna Integration</a>
 * @see <a href="http://app.usekahuna.com/tap/getstarted/android/">Kahuna Android SDK</a>
 */
public class KahunaIntegration extends AbstractIntegration<Void> {
  static final String KAHUNA_KEY = "Kahuna";

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    if (!isOnClassPath("android.support.v4.app.Fragment")) {
      throw new IllegalStateException("Kahuna requires the support library to be bundled.");
    }
    KahunaAnalytics.onAppCreate(context, settings.getString("apiKey"),
        settings.getString("pushSenderId"));
    KahunaAnalytics.setDebugMode(logLevel == INFO || logLevel == VERBOSE);
  }

  @Override public String key() {
    return KAHUNA_KEY;
  }

  @Override public boolean onActivityStarted(Activity activity) {
    KahunaAnalytics.start();
    return true;
  }

  @Override public boolean onActivityStopped(Activity activity) {
    KahunaAnalytics.stop();
    return true;
  }

  @Override public boolean identify(IdentifyPayload identify) {
    String username = identify.traits().username();
    KahunaAnalytics.setUsernameAndEmail(isNullOrEmpty(username) ? identify.userId() : username,
        identify.traits().email());

    KahunaAnalytics.setUserCredential(USERNAME_KEY, identify.traits().username());
    KahunaAnalytics.setUserCredential(EMAIL_KEY, identify.traits().email());
    KahunaAnalytics.setUserCredential(FACEBOOK_KEY, identify.traits().getString("facebook"));
    KahunaAnalytics.setUserCredential(TWITTER_KEY, identify.traits().getString("twitter"));
    KahunaAnalytics.setUserCredential(LINKEDIN_KEY, identify.traits().getString("linkedin"));

    KahunaAnalytics.setUserAttributes(identify.traits().toStringMap());
    return true;
  }

  @Override public boolean track(TrackPayload track) {
    KahunaAnalytics.trackEvent(track.event(), track.properties().getInt("count", 0),
        (int) track.properties().value());
    return true;
  }
}
