package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.moe.pushlibrary.MoEHelper;
import com.moe.pushlibrary.utils.MoEHelperConstants;
import com.segment.analytics.Analytics;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Set;

import static com.segment.analytics.internal.Utils.hasPermission;

/**
 * MoEngage is an advanced mobile marketing and engagement tool which has a wide range of
 * features. It helps user retention and increases churn.
 *
 * @see <a href="http://www.moengage.com/">MoEngage</a>
 * @see <a href="https://segment.com/docs/integrations/moengage/">MoEngage Integration</a>
 * @see <a href="http://docs.moengage.com/en/latest/android.html">MoEngage Android SDK</a>
 */
public class MoEngageIntegration extends AbstractIntegration<MoEHelper> {

  private static final String KEY_MOENGAGE = "MoEngage";

  MoEHelper helper;

  private static final String ANONYMOUS_ID_KEY = "anonymousId";
  private static final String EMAIL_KEY = "email";
  private static final String USER_ID_KEY = "userId";
  private static final String NAME_KEY = "name";
  private static final String PHONE_KEY = "phone";
  private static final String BIRTHDAY_KEY = "birthday";
  private static final String FIRST_NAME_KEY = "firstName";
  private static final String GENDER_KEY = "gender";
  private static final String LAST_NAME_KEY = "lastName";
  private static final String USER_ATTR_SEGMENT_AID = "USER_ATTRIBUTE_SEGMENT_ID";

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    Context context = analytics.getApplication();
    if (!hasPermission(context, "com.google.android.c2dm.permission.RECEIVE")) {
      throw new IllegalStateException(
          "MoEngage requires com.google.android.c2dm.permission.RECEIVE permission");
    }
    Analytics.LogLevel logLevel = analytics.getLogLevel();
    if (logLevel == Analytics.LogLevel.VERBOSE || logLevel == Analytics.LogLevel.INFO) {
      MoEHelper.APP_DEBUG = true;
    }
  }

  @Override public String key() {
    return KEY_MOENGAGE;
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);
    helper = new MoEHelper(activity);
    if (null != savedInstanceState) {
      helper.onRestoreInstanceState(savedInstanceState);
    }
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    helper.onStart(activity);
  }

  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    helper.onResume(activity);
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    helper.onPause(activity);
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    helper.onStop(activity);
  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    super.onActivitySaveInstanceState(activity, outState);
    helper.onSaveInstanceState(outState);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    Traits tr = identify.traits();
    if (null != tr && !tr.isEmpty()) {
      HashMap<String, Object> processedAttrs = new HashMap<String, Object>();
      Set<String> keys = tr.keySet();
      for (String key : keys) {
        if (null != key) {
          if (ANONYMOUS_ID_KEY.equals(key)) {
            processedAttrs.put(USER_ATTR_SEGMENT_AID, tr.get(key));
          } else if (EMAIL_KEY.equals(key)) {
            processedAttrs.put(MoEHelperConstants.USER_ATTRIBUTE_USER_EMAIL, tr.get(key));
          } else if (USER_ID_KEY.equals(key)) {
            processedAttrs.put(MoEHelperConstants.USER_ATTRIBUTE_UNIQUE_ID, tr.get(key));
          } else if (NAME_KEY.equals(key)) {
            processedAttrs.put(MoEHelperConstants.USER_ATTRIBUTE_USER_NAME, tr.get(key));
          } else if (PHONE_KEY.equals(key)) {
            processedAttrs.put(MoEHelperConstants.USER_ATTRIBUTE_USER_MOBILE, tr.get(key));
          } else if (FIRST_NAME_KEY.equals(key)) {
            processedAttrs.put(MoEHelperConstants.USER_ATTRIBUTE_USER_FIRST_NAME, tr.get(key));
          } else if (LAST_NAME_KEY.equals(key)) {
            processedAttrs.put(MoEHelperConstants.USER_ATTRIBUTE_USER_LAST_NAME, tr.get(key));
          } else if (GENDER_KEY.equals(key)) {
            processedAttrs.put(MoEHelperConstants.USER_ATTRIBUTE_USER_GENDER, tr.get(key));
          } else if (BIRTHDAY_KEY.equals(key)) {
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
            String birthDate = df.format(tr.birthday());
            processedAttrs.put(MoEHelperConstants.USER_ATTRIBUTE_USER_BDAY, birthDate);
          } else {
            processedAttrs.put(key, tr.get(key));
          }
        } else if (MoEHelper.APP_DEBUG) {
          Log.e(MoEHelper.TAG, "TRAIT KEY CANNOT BE NULL");
        }
      }
      helper.setUserAttribute(processedAttrs);
    }
    AnalyticsContext.Location location = identify.context().location();
    if (location != null) {
      helper.setUserLocation(location.latitude(), location.longitude());
    }
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    helper.trackEvent(track.event(), track.properties().toStringMap());
  }

  @Override public void reset() {
    super.reset();
    helper.logoutUser();
  }

  @Override public MoEHelper getUnderlyingInstance() {
    return helper;
  }
}
