package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import com.localytics.android.Localytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.Collections;
import java.util.Map;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.isOnClassPath;

/**
 * Localytics is a general-purpose mobile analytics tool that measures customer acquisition, ad
 * attribution, retargeting campaigns and user actions in your mobile apps.
 *
 * @see <a href="http://www.localytics.com/">Localytics</a>
 * @see <a href="https://segment.com/docs/integrations/localytics/">Localytics Integration</a>
 * @see <a href="http://www.localytics.com/docs/android-integration/">Localytics Android SDK</a>
 */
public class LocalyticsIntegration extends AbstractIntegration<Void> {

  static final String LOCALYTICS_KEY = "Localytics";
  boolean hasSupportLibOnClassPath;
  ValueMap customDimensions;

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    Localytics.integrate(analytics.getApplication(), settings.getString("appKey"));
    LogLevel logLevel = analytics.getLogLevel();
    Localytics.setLoggingEnabled(logLevel == INFO || logLevel == VERBOSE);
    hasSupportLibOnClassPath = isOnClassPath("android.support.v4.app.FragmentActivity");
    customDimensions = settings.getValueMap("dimensions");
    if (customDimensions == null) {
      customDimensions = new ValueMap(Collections.<String, Object>emptyMap());
    }
  }

  @Override public String key() {
    return LOCALYTICS_KEY;
  }

  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);

    Localytics.openSession();
    Localytics.upload();

    if (hasSupportLibOnClassPath) {
      if (activity instanceof android.support.v4.app.FragmentActivity) {
        Localytics.setInAppMessageDisplayActivity(
            (android.support.v4.app.FragmentActivity) activity);
      }
    }

    Intent intent = activity.getIntent();
    if (intent != null) {
      Localytics.handleTestMode(intent);
    }
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);

    if (hasSupportLibOnClassPath) {
      if (activity instanceof android.support.v4.app.FragmentActivity) {
        Localytics.dismissCurrentInAppMessage();
        Localytics.clearInAppMessageDisplayActivity();
      }
    }

    Localytics.closeSession();
    Localytics.upload();
  }

  @Override public void flush() {
    super.flush();

    Localytics.upload();
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);

    setContext(identify.context());
    Traits traits = identify.traits();

    String userId = traits.userId();
    if (!isNullOrEmpty(userId)) {
      Localytics.setCustomerId(identify.userId());
    }
    String email = traits.email();
    if (!isNullOrEmpty(email)) {
      Localytics.setIdentifier("email", email);
    }
    String name = traits.name();
    if (!isNullOrEmpty(name)) {
      Localytics.setIdentifier("customer_name", name);
    }
    setCustomDimensions(customDimensions, traits);

    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      Localytics.setProfileAttribute(entry.getKey(), String.valueOf(entry.getValue()),
          Localytics.ProfileScope.APPLICATION);
    }
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);

    setContext(screen.context());
    Localytics.tagScreen(screen.event());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    Properties props = track.properties();
    setContext(track.context());
    Localytics.tagEvent(track.event(), props.toStringMap());
    setCustomDimensions(customDimensions, props);
  }

  private void setContext(AnalyticsContext context) {
    if (isNullOrEmpty(context)) {
      return;
    }

    AnalyticsContext.Location location = context.location();
    if (location != null) {
      Location androidLocation = new Location("Segment");
      androidLocation.setLongitude(location.longitude());
      androidLocation.setLatitude(location.latitude());
      androidLocation.setSpeed((float) location.speed());
      Localytics.setLocation(androidLocation);
    }
  }

  private void setCustomDimensions(ValueMap customDimensions, ValueMap valueMap) {
    for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
      String propKey = entry.getKey();
      if (customDimensions.containsKey(propKey)) {
        Localytics.setCustomDimension(customDimensions.getInt(propKey, 0),
            String.valueOf(entry.getValue()));
      }
    }
  }
}
