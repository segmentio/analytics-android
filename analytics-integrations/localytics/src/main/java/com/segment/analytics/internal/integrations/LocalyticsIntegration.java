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
import com.segment.analytics.internal.Log;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.Collections;
import java.util.Map;

import static com.localytics.android.Localytics.ProfileScope.APPLICATION;
import static com.segment.analytics.Analytics.LogLevel;
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
  Log log;

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    log = analytics.getLogger().newLogger(LOCALYTICS_KEY);

    boolean loggingEnabled = log.logLevel.ordinal() >= LogLevel.VERBOSE.ordinal();
    Localytics.setLoggingEnabled(loggingEnabled);
    log.verbose("Localytics.setLoggingEnabled(%s);", loggingEnabled);

    String appKey = settings.getString("appKey");
    Localytics.integrate(analytics.getApplication(), appKey);
    log.verbose("Localytics.integrate(context, %s);", appKey);

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
    log.verbose("Localytics.openSession();");

    Localytics.upload();
    log.verbose("Localytics.upload();");

    if (hasSupportLibOnClassPath) {
      if (activity instanceof android.support.v4.app.FragmentActivity) {
        Localytics.setInAppMessageDisplayActivity(
            (android.support.v4.app.FragmentActivity) activity);
        log.verbose("Localytics.setInAppMessageDisplayActivity(activity);");
      }
    }

    Intent intent = activity.getIntent();
    if (intent != null) {
      Localytics.handleTestMode(intent);
      log.verbose("Localytics.handleTestMode(%s);", intent);
    }
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);

    if (hasSupportLibOnClassPath) {
      if (activity instanceof android.support.v4.app.FragmentActivity) {
        Localytics.dismissCurrentInAppMessage();
        log.verbose("Localytics.dismissCurrentInAppMessage();");
        Localytics.clearInAppMessageDisplayActivity();
        log.verbose("Localytics.clearInAppMessageDisplayActivity();");
      }
    }

    Localytics.closeSession();
    log.verbose("Localytics.closeSession();");
    Localytics.upload();
    log.verbose("Localytics.upload();");
  }

  @Override public void flush() {
    super.flush();

    Localytics.upload();
    log.verbose("Localytics.upload();");
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);

    setContext(identify.context());
    Traits traits = identify.traits();

    String userId = identify.userId();
    if (!isNullOrEmpty(userId)) {
      Localytics.setCustomerId(userId);
      log.verbose("Localytics.setCustomerId(%s);", userId);
    }
    String email = traits.email();
    if (!isNullOrEmpty(email)) {
      Localytics.setIdentifier("email", email);
      log.verbose("Localytics.setIdentifier(\"email\", %s);", email);
    }
    String name = traits.name();
    if (!isNullOrEmpty(name)) {
      Localytics.setIdentifier("customer_name", name);
      log.verbose("Localytics.setIdentifier(\"customer_name\", %s);", name);
    }
    setCustomDimensions(traits);

    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      String key = entry.getKey();
      String value = String.valueOf(entry.getValue());
      Localytics.setProfileAttribute(key, value, APPLICATION);
      log.verbose("Localytics.setProfileAttribute(%s, %s, %s);", key, value, APPLICATION);
    }
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);

    setContext(screen.context());

    String event = screen.event();
    Localytics.tagScreen(event);
    log.verbose("Localytics.tagScreen(%s);", event);
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    setContext(track.context());

    String event = track.event();
    Properties properties = track.properties();
    Map<String, String> stringProps = properties.toStringMap();
    // Convert revenue to cents.
    // http://docs.localytics.com/index.html#Dev/Instrument/customer-ltv.html
    final long revenue = (long) (properties.revenue() * 100);

    if (revenue != 0) {
      Localytics.tagEvent(event, stringProps, revenue);
      log.verbose("Localytics.tagEvent(%s, %s, %s);", event, stringProps, revenue);
    } else {
      Localytics.tagEvent(event, stringProps);
      log.verbose("Localytics.tagEvent(%s, %s);", event, stringProps);
    }

    setCustomDimensions(properties);
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
      log.verbose("Localytics.setLocation(%s);", androidLocation);
    }
  }

  private void setCustomDimensions(ValueMap dimensions) {
    for (Map.Entry<String, Object> entry : dimensions.entrySet()) {
      String dimensionKey = entry.getKey();
      if (customDimensions.containsKey(dimensionKey)) {
        int dimension = customDimensions.getInt(dimensionKey, 0);
        String value = String.valueOf(entry.getValue());
        Localytics.setCustomDimension(dimension, value);
        log.verbose("Localytics.setCustomDimension(%s, %s);", dimension, value);
      }
    }
  }
}
