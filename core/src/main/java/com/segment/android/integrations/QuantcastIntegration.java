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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.quantcast.measurement.service.QuantcastClient;
import com.segment.android.Logger;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;

import static com.segment.android.utils.Utils.isNullOrEmpty;

public class QuantcastIntegration extends SimpleIntegration {
  private static final String API_KEY = "apiKey";

  private String apiKey;

  @Override
  public String getKey() {
    return "Quantcast";
  }

  @Override
  public String[] getRequiredPermissions() {
    return new String[] {
        Manifest.permission.READ_PHONE_STATE
    };
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {
    if (isNullOrEmpty(settings.getString(API_KEY))) {
      throw new InvalidSettingsException(API_KEY, "Quantcast requires the apiKey setting.");
    }
  }

  @Override
  public void onCreate(Context context) {
    checkPermission(context);

    EasyJSONObject settings = this.getSettings();
    apiKey = settings.getString(API_KEY);

    QuantcastClient.enableLogging(Logger.isLogging());

    ready();
  }

  @Override
  public void onActivityStart(Activity activity) {
    if (!checkPermission(activity)) return;
    QuantcastClient.activityStart(activity, apiKey, null, null);
  }

  @Override
  public void onActivityStop(Activity activity) {
    if (!checkPermission(activity)) return;
    QuantcastClient.activityStop();
  }

  @Override
  public void identify(Identify identify) {
    if (!hasPermission) return;
    String userId = identify.getUserId();
    QuantcastClient.recordUserIdentifier(userId);
  }

  @Override
  public void screen(Screen screen) {
    if (!hasPermission) return;
    event("Viewed " + screen.getName() + " Screen", screen.getProperties());
  }

  @Override
  public void track(Track track) {
    if (!hasPermission) return;
    event(track.getEvent(), track.getProperties());
  }

  private void event(String name, Props properties) {
    QuantcastClient.logEvent(name);
  }
}
