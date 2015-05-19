package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.os.Bundle;

import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.uxcam.UXCam;
/**
 * UXCam allows you to eliminate customer struggle and improve user experience by capturing
 * and visualizing screen video and user interaction data.
 *
 * @see <a href="http://www.uxcam.com/">UXCam</a>
 * @see <a href="https://dashboard.uxcam.com/docs#/android">UXCam Android SDK</a>
 */
public class UXCamIntegration extends AbstractIntegration<UXCam> {

  static final String UXCAM_KEY = "UXCam";
  String accountKey;

  @Override public void initialize(Analytics analytics, ValueMap settings)
          throws IllegalStateException {
    accountKey = settings.getString("accountKey");
  }

  @Override public UXCam getUnderlyingInstance() {
    return null;
  }

  @Override public String key() {
    return UXCAM_KEY;
  }

  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    UXCam.startWithKeyForSegment(activity, accountKey);
  }

}
