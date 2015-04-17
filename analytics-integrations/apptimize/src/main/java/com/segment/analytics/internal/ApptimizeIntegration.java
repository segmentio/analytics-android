package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.content.Context;
import com.apptimize.Apptimize;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.internal.Utils.hasPermission;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Apptimize allows you to instantly update your native app without waiting for
 * App or Play Store approvals, and easily see if the change improved the app
 * with robust A/B testing analytics.
 *
 * @see <a href="http://www.apptimize.com/">Apptimize</a>
 * @see <a href="https://segment.com/docs/integrations/apptimize/">Apptimize Integration</a>
 */
public class ApptimizeIntegration extends AbstractIntegration<Void> {
  static final String APPTIMIZE_KEY = "Apptimize";

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    if (!hasPermission(context, Manifest.permission.INTERNET)) {
      throw new IllegalStateException("Apptimize requires INTERNET permission");
    }
    Apptimize.setup(context, settings.getString("appkey"));
  }

  @Override public String key() {
    return APPTIMIZE_KEY;
  }

  void addAttributeIfNotZero(String name, int value) {
    if (value > 0) {
      Apptimize.setUserAttribute(name, value);
    }
  }

  void addAttributeIfNotNull(String name, String value) {
    if (!isNullOrEmpty(value)) {
      Apptimize.setUserAttribute(name, value);
    }
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    Apptimize.setUserAttribute("user_id", identify.userId());

    Traits traits = identify.traits();
    addAttributeIfNotZero("age", traits.age());
    addAttributeIfNotNull("createdAt", traits.createdAt());
    addAttributeIfNotNull("description", traits.description());
    addAttributeIfNotNull("email", traits.email());
    addAttributeIfNotZero("employees", (int) traits.employees());
    addAttributeIfNotNull("fax", traits.fax());
    addAttributeIfNotNull("firstName", traits.firstName());
    addAttributeIfNotNull("gender", traits.gender());
    addAttributeIfNotNull("lastName", traits.lastName());
    addAttributeIfNotNull("phone", traits.phone());
    addAttributeIfNotNull("title", traits.title());
    addAttributeIfNotNull("username", traits.username());
    addAttributeIfNotNull("website", traits.website());
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    if (track.properties().containsKey("value")) {
      Object value = track.properties().get("value");
      if (value instanceof Double) {
        Apptimize.track(track.event(), (double) value);
        return;
      }
      if (value instanceof Number) {
        Apptimize.track(track.event(), ((Number) value).doubleValue());
        return;
      }
    }
    Apptimize.track(track.event());
  }


  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    Apptimize.track(screen.event());
  }

}
