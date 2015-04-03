package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Client;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.Map;

import static com.segment.analytics.Analytics.LogLevel;

/**
 * Bugsnag is an error tracking service for websites and mobile apps. It automatically captures any
 * errors in your code so that you can find them and resolve them as quickly as possible.
 *
 * @see <a href="https://bugsnag.com/">Bugsnag</a>
 * @see <a href="https://segment.com/docs/integrations/bugsnag/">Bugsnag Integration</a>
 * @see <a href="https://github.com/bugsnag/bugsnag-android">Bugsnag Android SDK</a>
 */
public class BugsnagIntegration extends AbstractIntegration<Client> {
  static final String BUGSNAG_KEY = "Bugsnag";

  @Override public void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException {
    Bugsnag.init(context, settings.getString("apiKey"));
  }

  @Override public Client getUnderlyingInstance() {
    return Bugsnag.getClient();
  }

  @Override public String key() {
    return BUGSNAG_KEY;
  }

  @Override public boolean onActivityCreated(Activity activity, Bundle savedInstanceState) {
    Bugsnag.setContext(activity.getLocalClassName());
    return true;
  }

  @Override public boolean identify(IdentifyPayload identify) {
    Traits traits = identify.traits();
    Bugsnag.setUser(traits.userId(), traits.email(), traits.name());
    final String userKey = "User";
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      Bugsnag.addToTab(userKey, entry.getKey(), entry.getValue());
    }
    return true;
  }

  @Override public boolean screen(ScreenPayload screen) {
    Bugsnag.leaveBreadcrumb(String.format(VIEWED_EVENT_FORMAT, screen.event()));
    return true;
  }

  @Override public boolean track(TrackPayload track) {
    Bugsnag.leaveBreadcrumb(track.event());
    return true;
  }
}
