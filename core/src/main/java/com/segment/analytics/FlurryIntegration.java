package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;

import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * Flurry is the most popular analytics tool for mobile apps because it has a wide assortment of
 * features. It also helps you advertise to the right audiences with your apps.
 *
 * @see <a href="http://www.flurry.com/">Flurry</a>
 * @see <a href="https://segment.io/docs/integrations/flurry/">Flurry Integration</a>
 * @see <a href="http://support.flurry.com/index.php?title=Analytics/GettingStarted/Android">Flurry
 * Android SDK</a>
 */
class FlurryIntegration extends AbstractIntegration<Void> {
  static final String FLURRY_KEY = "Flurry";
  String apiKey;

  @Override void initialize(Context context, JsonMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    apiKey = settings.getString("apiKey");
    FlurryAgent.setContinueSessionMillis(settings.getInt("sessionContinueSeconds", 10) * 1000);
    FlurryAgent.setCaptureUncaughtExceptions(
        settings.getBoolean("captureUncaughtExceptions", false));
    FlurryAgent.setUseHttps(settings.getBoolean("useHttps", true));
    FlurryAgent.setLogEnabled(debuggingEnabled);
  }

  @Override void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    FlurryAgent.onStartSession(activity, apiKey);
  }

  @Override void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    FlurryAgent.onEndSession(activity);
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    // todo: verify behaviour here, iOS SDK only does pageView, not event
    FlurryAgent.onPageView();
    FlurryAgent.logEvent(screen.event(), screen.properties().toStringMap());
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    FlurryAgent.logEvent(track.event(), track.properties().toStringMap());
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    Traits traits = identify.traits();
    FlurryAgent.setUserId(identify.userId());
    int age = traits.age();
    if (age > 0) {
      FlurryAgent.setAge(age);
    }
    String gender = traits.gender();
    if (!isNullOrEmpty(gender)) {
      if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("m")) {
        FlurryAgent.setGender(Constants.MALE);
      } else if (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("f")) {
        FlurryAgent.setGender(Constants.FEMALE);
      } else {
        FlurryAgent.setGender(Constants.UNKNOWN);
      }
    }
    AnalyticsContext.Location location = identify.context().location();
    if (location != null) {
      FlurryAgent.setLocation((float) location.latitude(), (float) location.longitude());
    }
  }

  @Override Void getUnderlyingInstance() {
    return null;
  }

  @Override String key() {
    return FLURRY_KEY;
  }
}
