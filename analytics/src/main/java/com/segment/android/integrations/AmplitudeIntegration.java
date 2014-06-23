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
import android.text.TextUtils;
import com.amplitude.api.Amplitude;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;

public class AmplitudeIntegration extends SimpleIntegration {

  private static class SettingKey {
    private static final String API_KEY = "apiKey";
  }

  @Override
  public String getKey() {
    return "Amplitude";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {

    if (TextUtils.isEmpty(settings.getString(SettingKey.API_KEY))) {
      throw new InvalidSettingsException(SettingKey.API_KEY,
          "Amplitude requires the apiKey setting.");
    }
  }

  @Override
  public void onCreate(Context context) {

    EasyJSONObject settings = this.getSettings();
    String apiKey = settings.getString(SettingKey.API_KEY);

    Amplitude.initialize(context, apiKey);

    ready();
  }

  @Override
  public void onActivityStart(Activity activity) {
    Amplitude.startSession();
  }

  @Override
  public void onActivityStop(Activity activity) {
    Amplitude.endSession();
  }

  @Override
  public void identify(Identify identify) {
    String userId = identify.getUserId();
    Traits traits = identify.getTraits();

    Amplitude.setUserId(userId);
    Amplitude.setUserProperties(traits);
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
    if (properties != null && properties.has("revenue")) {
      double revenue = properties.getDouble("revenue", 0.0);
      Amplitude.logRevenue(revenue);
    }

    Amplitude.logEvent(name, properties);
  }

  @Override
  public void flush() {
    Amplitude.uploadEvents();
  }
}
