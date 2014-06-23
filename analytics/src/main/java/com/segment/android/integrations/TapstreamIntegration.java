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

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;
import java.util.Iterator;
import org.OpenUDID.OpenUDID_manager;

public class TapstreamIntegration extends SimpleIntegration {

  private static class SettingKey {
    private static final String ACCOUNT_NAME = "accountName";
    private static final String SDK_SECRET = "sdkSecret";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {
    if (TextUtils.isEmpty(settings.getString(SettingKey.ACCOUNT_NAME))) {
      throw new InvalidSettingsException(SettingKey.ACCOUNT_NAME, "accountName required.");
    }
    if (TextUtils.isEmpty(settings.getString(SettingKey.SDK_SECRET))) {
      throw new InvalidSettingsException(SettingKey.SDK_SECRET, "sdkSecret required.");
    }
  }

  @Override
  public String getKey() {
    return "Tapstream";
  }

  private void initialize(Context context) {
    EasyJSONObject settings = this.getSettings();
    String accountName = settings.getString(SettingKey.ACCOUNT_NAME);
    String sdkSecret = settings.getString(SettingKey.SDK_SECRET);

    Config config = new Config();
    config.setOpenUdid(OpenUDID_manager.getOpenUDID());

    Tapstream.create((Application) context.getApplicationContext(), accountName, sdkSecret, config);

    ready();
  }

  @Override
  public void onCreate(Context context) {
    initialize(context);
  }

  @Override
  public void screen(Screen screen) {
    Tapstream.getInstance()
        .fireEvent(makeEvent("screen-" + screen.getName(), screen.getProperties()));
  }

  @Override
  public void track(Track track) {
    Tapstream.getInstance().fireEvent(makeEvent(track.getEvent(), track.getProperties()));
  }

  private Event makeEvent(String name, Props properties) {
    Event e = new Event(name, false);
    if (properties != null) {
      Iterator<?> iter = properties.keys();
      while (iter.hasNext()) {
        String key = (String) iter.next();
        e.addPair(key, properties.get(key));
      }
    }
    return e;
  }
}