package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Flurry is the most popular analytics tool for mobile apps because it has a wide assortment of
 * features. It also helps you advertise to the right audiences with your apps.
 *
 * @see <a href="http://www.flurry.com/">Flurry</a>
 * @see <a href="https://segment.com/docs/integrations/flurry/">Flurry Integration</a>
 * @see <a href="http://support.flurry.com/index.php?title=Analytics/GettingStarted/Android">Flurry
 * Android SDK</a>
 */
public class FlurryIntegration extends AbstractIntegration<Void> {

  static final String FLURRY_KEY = "Flurry";

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    // TODO: is Google Play Services and the support lib absolutely required?
    FlurryAgent.setContinueSessionMillis(settings.getInt("sessionContinueSeconds", 10) * 1000);
    FlurryAgent.setCaptureUncaughtExceptions(
        settings.getBoolean("captureUncaughtExceptions", false));
    FlurryAgent.setReportLocation(settings.getBoolean("reportLocation", true));
    FlurryAgent.setLogEnabled(logLevel == INFO || logLevel == VERBOSE);
    FlurryAgent.setLogEvents(logLevel == VERBOSE);
    FlurryAgent.init(context, settings.getString("apiKey"));
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    FlurryAgent.onStartSession(activity);
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    FlurryAgent.onEndSession(activity);
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    // todo: verify behaviour here, iOS SDK only does pageView, not event
    FlurryAgent.onPageView();
    FlurryAgent.logEvent(screen.event(), screen.properties().toStringMap());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    FlurryAgent.logEvent(track.event(), track.properties().toStringMap());
  }

  @Override public void identify(IdentifyPayload identify) {
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

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public String key() {
    return FLURRY_KEY;
  }
}
