/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android.integrations;

import android.app.Activity;
import android.content.Context;
import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;
import com.segment.android.Logger;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.segment.android.utils.Utils.isNullOrEmpty;

public class FlurryIntegration extends SimpleIntegration {

  private static class SettingKey {

    private static final String API_KEY = "apiKey";

    private static final String SESSION_LENGTH = "sessionLength";
    private static final String CAPTURE_UNCAUGHT_EXCEPTIONS = "captureUncaughtExceptions";
    private static final String USE_HTTPS = "useHttps";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {
    if (isNullOrEmpty(settings.getString(SettingKey.API_KEY))) {
      throw new InvalidSettingsException(SettingKey.API_KEY, "API Key (apiKey) required.");
    }
  }

  @Override
  public String getKey() {
    return "Flurry";
  }

  private void initialize() {

    EasyJSONObject settings = this.getSettings();

    int sessionLength = settings.getInt(SettingKey.SESSION_LENGTH, 10000);
    boolean captureUncaughtExceptions =
        settings.getBoolean(SettingKey.CAPTURE_UNCAUGHT_EXCEPTIONS, false);
    boolean useHttps = settings.getBoolean(SettingKey.USE_HTTPS, false);

    FlurryAgent.setContinueSessionMillis(sessionLength);
    FlurryAgent.setCaptureUncaughtExceptions(captureUncaughtExceptions);
    FlurryAgent.setUseHttps(useHttps);
  }

  @Override
  public void onCreate(Context context) {
    initialize();
    ready(); // should be ready so that onActivityStart(..) can run
  }

  @Override
  public void onActivityStart(Activity activity) {
    EasyJSONObject settings = this.getSettings();
    String apiKey = settings.getString(SettingKey.API_KEY);
    FlurryAgent.onStartSession(activity, apiKey);
  }

  @Override
  public void onActivityStop(Activity activity) {
    try {
      FlurryAgent.onEndSession(activity);
    } catch (NullPointerException e) {
      Logger.w(e, "Flurry Agent's #onEndSession threw a NullPointerException.");
    }
  }

  @Override
  public void identify(Identify identify) {
    Traits traits = identify.getTraits();

    String gender = traits.getString("gender");
    if (!isNullOrEmpty(gender)) {
      if (gender.equalsIgnoreCase("male")) {
        FlurryAgent.setGender(Constants.MALE);
      } else if (gender.equalsIgnoreCase("female")) {
        FlurryAgent.setGender(Constants.FEMALE);
      }
    }

    Integer age = traits.getInt("age", null);
    if (age != null && age > 0) FlurryAgent.setAge(age);

    String userId = identify.getUserId();
    FlurryAgent.setUserId(userId);
  }

  @Override
  public void screen(Screen screen) {
    // increment flurry's page view count
    FlurryAgent.onPageView();
    // track a "Viewed SCREEN" event
    event("Viewed " + screen.getName() + " Screen", screen.getProperties());
  }

  @Override
  public void track(Track track) {
    event(track.getEvent(), track.getProperties());
  }

  private void event(String name, Props properties) {
    FlurryAgent.logEvent(name, toMap(properties));
  }

  private Map<String, String> toMap(Props properties) {
    Map<String, String> map = new HashMap<String, String>();

    if (properties != null) {
      @SuppressWarnings("unchecked") Iterator<String> it = properties.keys();
      while (it.hasNext()) {
        String key = it.next();
        String value = "" + properties.get(key);
        if (value.length() > 255) value = value.substring(0, 255);
        map.put(key, value);
      }
    }

    return map;
  }
}

