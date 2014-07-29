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
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import java.util.HashMap;
import java.util.Map;
import ly.count.android.api.Countly;

import static com.segment.android.utils.Utils.isNullOrEmpty;

public class CountlyIntegration extends SimpleIntegration {

  private static class SettingKey {

    private static final String SERVER_URL = "serverUrl";
    private static final String APP_KEY = "appKey";
  }

  @Override
  public String getKey() {
    return "Countly";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {
    if (isNullOrEmpty(settings.getString(SettingKey.SERVER_URL))) {
      throw new InvalidSettingsException(SettingKey.SERVER_URL,
          "Countly requires the serverUrl setting.");
    }

    if (isNullOrEmpty(settings.getString(SettingKey.APP_KEY))) {
      throw new InvalidSettingsException(SettingKey.APP_KEY,
          "Amplitude requires the appKey setting.");
    }
  }

  @Override
  public void onCreate(Context context) {
    EasyJSONObject settings = this.getSettings();
    String serverUrl = settings.getString(SettingKey.SERVER_URL);
    String appKey = settings.getString(SettingKey.APP_KEY);

    Countly.sharedInstance().init(context, serverUrl, appKey);

    ready();
  }

  @Override
  public void onActivityStart(Activity activity) {
    Countly.sharedInstance().onStart();
  }

  @Override
  public void onActivityStop(Activity activity) {
    Countly.sharedInstance().onStop();
  }

  @Override
  public void screen(Screen screen) {
    event("Viewed " + screen.getName() + " Screen", screen.getProperties());
  }

  @Override
  public void track(Track track) {
    event(track.getEvent(), track.getProperties());
  }

  private void event(String name, Props properties) {
    Map<String, String> segmentation = new HashMap<String, String>();
    int count = 0;

    if (properties != null) {
      segmentation = properties.toStringMap();
      if (properties.has("sum")) {
        count = properties.getInt("sum", 0);
      }
    }

    Countly.sharedInstance().recordEvent(name, segmentation, count);
  }
}
