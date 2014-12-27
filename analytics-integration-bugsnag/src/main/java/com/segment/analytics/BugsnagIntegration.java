package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Client;
import java.util.Map;

/**
 * Bugsnag is an error tracking service for websites and mobile apps. It automatically captures any
 * errors in your code so that you can find them and resolve them as quickly as possible.
 *
 * @see <a href="https://bugsnag.com/">Bugsnag</a>
 * @see <a href="https://Segment/docs/integrations/bugsnag/">Bugsnag Integration</a>
 * @see <a href="https://github.com/bugsnag/bugsnag-android">Bugsnag Android SDK</a>
 */
public class BugsnagIntegration extends AbstractIntegration<Client> {
  static final String BUGSNAG_KEY = "Bugsnag";

  @Override void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    Bugsnag.init(context, settings.getString("apiKey"));
  }

  @Override Client getUnderlyingInstance() {
    return Bugsnag.getClient();
  }

  @Override String key() {
    return BUGSNAG_KEY;
  }

  @Override void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);
    Bugsnag.setContext(activity.getLocalClassName());
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

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    Bugsnag.leaveBreadcrumb(String.format(VIEWED_EVENT_FORMAT, screen.event()));
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    Bugsnag.leaveBreadcrumb(track.event());
  }
}
