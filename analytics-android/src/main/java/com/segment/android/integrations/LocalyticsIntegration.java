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
import com.localytics.android.LocalyticsSession;
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

public class LocalyticsIntegration extends SimpleIntegration {

  private static class SettingKey {

    private static final String APP_KEY = "appKey";
  }

  private LocalyticsSession localyticsSession;

  @Override
  public String getKey() {
    return "Localytics";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {
    if (isNullOrEmpty(settings.getString(SettingKey.APP_KEY))) {
      throw new InvalidSettingsException(SettingKey.APP_KEY,
          "Localytics requires the appKey setting.");
    }
  }

  @Override
  public void onCreate(Context context) {
    // docs: http://www.localytics.com/docs/android-integration/
    EasyJSONObject settings = this.getSettings();
    String appKey = settings.getString(SettingKey.APP_KEY);

    this.localyticsSession = new LocalyticsSession(context, appKey);

    this.localyticsSession.open();
    this.localyticsSession.upload();

    ready();
  }

  @Override
  public void onActivityResume(Activity activity) {
    if (this.localyticsSession != null) this.localyticsSession.open();
  }

  @Override
  public void onActivityPause(Activity activity) {
    if (this.localyticsSession != null) {
      this.localyticsSession.close();
      this.localyticsSession.upload();
    }
  }

  @Override
  public void identify(Identify identify) {
    String userId = identify.getUserId();
    Traits traits = identify.getTraits();

    this.localyticsSession.setCustomerId(userId);

    if (traits != null) {
      if (traits.has("email")) this.localyticsSession.setCustomerEmail(traits.getString("email"));

      if (traits.has("name")) this.localyticsSession.setCustomerEmail(traits.getString("name"));

      @SuppressWarnings("unchecked") Iterator<String> it = traits.keys();
      while (it.hasNext()) {
        String key = it.next();
        String value = "" + traits.get(key);
        this.localyticsSession.setCustomerData(key, value);
      }
    }
  }

  @Override
  public void screen(Screen screen) {
    String screenName = screen.getName();
    this.localyticsSession.tagScreen(screenName);
  }

  @Override
  public void track(Track track) {
    String event = track.getEvent();
    Props properties = track.getProperties();

    Map<String, String> map = new HashMap<String, String>();
    if (properties != null) map = properties.toStringMap();

    this.localyticsSession.tagEvent(event, map);
  }

  @Override
  public void flush() {
    if (this.localyticsSession != null) this.localyticsSession.upload();
  }
}
