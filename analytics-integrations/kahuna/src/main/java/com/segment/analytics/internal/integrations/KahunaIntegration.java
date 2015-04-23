package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.kahuna.sdk.KahunaAnalytics;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static com.kahuna.sdk.KahunaUserCredentialKeys.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.GOOGLE_PLUS_ID;
import static com.kahuna.sdk.KahunaUserCredentialKeys.INSTALL_TOKEN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USERNAME_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USER_ID_KEY;
import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
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
  static final String SEGMENT_REVENUE_KEY = "revenue";
  static final String SEGMENT_QUANTITY_KEY = "quantity";
  boolean trackAllPages;

  static final Set<String> SUPPORTED_KAHUNA_CREDENTIALS = new HashSet<>(Arrays.asList(USERNAME_KEY,
          EMAIL_KEY, FACEBOOK_KEY, TWITTER_KEY, LINKEDIN_KEY, INSTALL_TOKEN_KEY, GOOGLE_PLUS_ID));

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    if (!isOnClassPath("android.support.v4.app.Fragment")) {
      throw new IllegalStateException("Kahuna requires the support library to be bundled.");
    }
    trackAllPages = settings.getBoolean("trackAllPages", false);

    KahunaAnalytics.onAppCreate(context, settings.getString("apiKey"),
             settings.getString("pushSenderId"));
    KahunaAnalytics.setDebugMode(logLevel == INFO || logLevel == VERBOSE);
  }

  @Override public String key() {
    return KAHUNA_KEY;
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    KahunaAnalytics.start();
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    KahunaAnalytics.stop();
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);

    KahunaAnalytics.setUserCredential(USER_ID_KEY, identify.userId());

    Traits identityTraits = identify.traits();
    Map<String, String> userAttributes = new HashMap<>();
    for(String key : identityTraits.keySet()) {
      if(identityTraits.get(key) == null) {
        continue; // Skip null value objects.
      }
      else if(SUPPORTED_KAHUNA_CREDENTIALS.contains(key)) {
        KahunaAnalytics.setUserCredential(key, identityTraits.getString(key));
      }
      else {
        // We'll track unsupported Kahuna User Credentials as Kahuna User Attributes instead
        // and make sure to format any Date objects appropriately.
        Object value = identityTraits.get(key);
        if(value instanceof Date) {
          userAttributes.put(key, getISO8601StringForDate((Date) value));
        }
        else {
          userAttributes.put(key, String.valueOf(value));
        }
      }
    }
    KahunaAnalytics.setUserAttributes(userAttributes);
  }

  @Override public void track(TrackPayload track) {
    super.track(track);

    if (track == null || TextUtils.isEmpty(track.event()))
      return;

    String kahunaEventName = track.event();

    if (track.properties() == null) {
      KahunaAnalytics.trackEvent(kahunaEventName);
    } else {
      Object revenueObject = track.properties().get(SEGMENT_REVENUE_KEY);
      Object quantityObject = track.properties().get(SEGMENT_QUANTITY_KEY);
      if (revenueObject == null && quantityObject == null) {
        KahunaAnalytics.trackEvent(kahunaEventName);
      } else {
        int kahunaValue = (revenueObject == null) ? 0 : (int) track.properties().revenue() * 100;
        int kahunaCount = (quantityObject == null) ? 0 : track.properties().getInt(SEGMENT_QUANTITY_KEY, 0);
        KahunaAnalytics.trackEvent(kahunaEventName, kahunaCount, kahunaValue);
      }
    }
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);

    // TODO: Segment is still working on the trackAllPages setting, currently it is defaulted to false.
    if (trackAllPages) {
      if (screen != null && screen.name() != null) {
        KahunaAnalytics.trackEvent("Viewed " + screen.name() + " Screen");
      }
    }
  }

  @Override public void reset() {
    super.reset();

    KahunaAnalytics.logout();
  }

  @Override public void alias(AliasPayload alias) {
    KahunaAnalytics.setUserCredential(USER_ID_KEY, alias.userId());
  }

  private static String getISO8601StringForDate(Date date) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat.format(date);
  }
}
