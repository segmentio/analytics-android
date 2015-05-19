package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.os.Bundle;
import com.segment.analytics.Analytics;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.uxcam.UXCam;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * UXCam allows you to eliminate customer struggle and improve user experience by capturing
 * and visualizing screen video and user interaction data.
 *
 * @see <a href="http://www.uxcam.com/">UXCam</a>
 * @see <a href="https://dashboard.uxcam.com/docs#/android">UXCam Android SDK</a>
 */
public class UXCamIntegration extends AbstractIntegration<UXCam> {

  private static final String UXCAM_KEY = "UXCam";
  String accountKey;

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    accountKey = settings.getString("accountKey");
  }

  @Override public String key() {
    return UXCAM_KEY;
  }

  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    UXCam.startWithKeyForSegment(activity, accountKey);
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    UXCam.tagScreenName(screen.event());
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    Traits traits = identify.traits();

    String userId = traits.userId();
    if (!isNullOrEmpty(userId)) {
      UXCam.tagUsersName(identify.userId());
    }
  }
}
