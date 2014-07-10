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
import com.bugsnag.android.Bugsnag;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Traits;
import java.util.Iterator;

import static com.segment.android.utils.Utils.isNullOrEmpty;

public class BugsnagIntegration extends SimpleIntegration {

  private static class SettingKey {

    private static final String API_KEY = "apiKey";

    private static final String USE_SSL = "useSSL";
  }

  @Override
  public String getKey() {
    return "Bugsnag";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {
    if (isNullOrEmpty(settings.getString(SettingKey.API_KEY))) {
      throw new InvalidSettingsException(SettingKey.API_KEY,
          "Bugsnag requires the setting apiKey.");
    }
  }

  @Override
  public void onCreate(Context context) {

    EasyJSONObject settings = this.getSettings();
    String apiKey = settings.getString(SettingKey.API_KEY);

    boolean useSsl = settings.getBoolean(SettingKey.USE_SSL, false);

    Bugsnag.setUseSSL(useSsl);

    Bugsnag.register(context, apiKey);

    ready();
  }

  @Override
  public void onActivityStart(Activity activity) {
    Bugsnag.setContext(activity.getLocalClassName());
  }

  @Override
  public void identify(Identify identify) {
    String userId = identify.getUserId();
    Traits traits = identify.getTraits();

    String email = traits.getString("email", "user@gmail.com");
    String name = traits.getString("name", "User Name");

    Bugsnag.setUser(userId, email, name);

    if (traits != null) {
      @SuppressWarnings("unchecked") Iterator<String> keys = traits.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        Object value = traits.get(key);
        Bugsnag.addToTab("User", key, value);
      }
    }
  }
}
