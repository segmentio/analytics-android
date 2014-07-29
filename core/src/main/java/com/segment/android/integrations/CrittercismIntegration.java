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

import android.content.Context;
import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;

import static com.segment.android.utils.Utils.isNullOrEmpty;

public class CrittercismIntegration extends SimpleIntegration {
  private static final String APP_ID = "appId";
  private static final String DELAY_SENDING_APP_LOAD = "delaySendingAppLoad";
  private static final String INCLUDE_VERSION_CODE = "includeVersionCode";
  private static final String SHOULD_INCLUDE_LOGCAT = "shouldCollectLogcat";

  @Override
  public String getKey() {
    return "Crittercism";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {

    if (isNullOrEmpty(settings.getString(APP_ID))) {
      throw new InvalidSettingsException(APP_ID, "Crittercism requires the appId setting.");
    }
  }

  @Override
  public void onCreate(Context context) {
    EasyJSONObject settings = this.getSettings();
    String appId = settings.getString(APP_ID);

    // docs: https://app.crittercism.com/developers/docs-optional-android
    CrittercismConfig config = new CrittercismConfig();
    // send app load data with Crittercism.sendAppLoadData()
    config.setDelaySendingAppLoad(settings.getBoolean(DELAY_SENDING_APP_LOAD, false));
    // necessary for collecting logcat data on Android Jelly Bean devices.
    config.setLogcatReportingEnabled(settings.getBoolean(SHOULD_INCLUDE_LOGCAT, false));
    // include version code in version name.
    config.setVersionCodeToBeIncludedInVersionString(
        settings.getBoolean(INCLUDE_VERSION_CODE, false));

    Crittercism.initialize(context, appId, config);

    ready();
  }

  @Override
  public void identify(Identify identify) {
    String userId = identify.getUserId();
    Traits traits = identify.getTraits();

    Crittercism.setUsername(userId);
    if (traits != null) {
      if (traits.has("name")) Crittercism.setUsername(traits.getString("name"));
      Crittercism.setMetadata(traits);
    }
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
    Crittercism.leaveBreadcrumb(name);
  }

  @Override
  public void flush() {
    Crittercism.sendAppLoadData();
  }
}
