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
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.Alias;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;
import com.segment.android.utils.Parameters;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.segment.android.utils.Utils.isNullOrEmpty;

public class MixpanelIntegration extends SimpleIntegration {

  private static class SettingKey {
    private static final String TOKEN = "token";
    private static final String PEOPLE = "people";
  }

  private MixpanelAPI mixpanel;

  @Override
  public String getKey() {
    return "Mixpanel";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {
    if (isNullOrEmpty(settings.getString(SettingKey.TOKEN))) {
      throw new InvalidSettingsException(SettingKey.TOKEN, "Mixpanel requires the token setting.");
    }
  }

  private boolean isMixpanelPeopleEnabled() {
    EasyJSONObject settings = this.getSettings();
    return settings.getBoolean(SettingKey.PEOPLE, false);
  }

  @Override
  public void onCreate(Context context) {

    EasyJSONObject settings = this.getSettings();
    String token = settings.getString(SettingKey.TOKEN);

    mixpanel = MixpanelAPI.getInstance(context, token);

    ready();
  }

  @SuppressWarnings("serial")
  private static final Map<String, String> traitsMapping = new HashMap<String, String>() {
    {
      this.put("created", "$created");
      this.put("email", "$email");
      this.put("firstName", "$first_name");
      this.put("lastName", "$last_name");
      this.put("lastSeen", "$last_seen");
      this.put("name", "$name");
      this.put("username", "$username");
    }
  };

  @Override
  public void identify(Identify identify) {
    String userId = identify.getUserId();
    Traits traits = identify.getTraits();

    EasyJSONObject mappedTraits = Parameters.move(traits, traitsMapping);

    mixpanel.identify(userId);

    if (traits != null) mixpanel.registerSuperProperties(mappedTraits);

    if (isMixpanelPeopleEnabled()) {
      MixpanelAPI.People people = mixpanel.getPeople();
      people.identify(userId);

      @SuppressWarnings("unchecked") Iterator<String> it = mappedTraits.keys();
      while (it.hasNext()) {
        String key = it.next();
        people.set(key, mappedTraits.get(key));
      }
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
    mixpanel.track(name, properties);

    if (isMixpanelPeopleEnabled()) {
      // consider the charge
      if (properties != null && properties.has("revenue")) {
        MixpanelAPI.People people = mixpanel.getPeople();
        double revenue = properties.getDouble("revenue", 0.0);
        people.trackCharge(revenue, properties);
      }
    }
  }

  @Override
  public void alias(Alias alias) {
    mixpanel.alias(alias.getUserId(), alias.getPreviousId());
  }

  @Override
  public void reset() {
    if (mixpanel != null) mixpanel.clearSuperProperties();
  }

  @Override
  public void flush() {
    if (mixpanel != null) mixpanel.flush();
  }
}
