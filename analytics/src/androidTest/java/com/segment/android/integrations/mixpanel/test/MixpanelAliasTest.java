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

package com.segment.android.integrations.mixpanel.test;

import android.util.Log;
import com.segment.android.Analytics;
import com.segment.android.Constants;
import com.segment.android.integration.BaseIntegrationInitializationActivity;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.MixpanelIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Options;
import com.segment.android.models.Props;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;
import java.util.Random;
import org.junit.Test;

public class MixpanelAliasTest extends BaseIntegrationInitializationActivity {

  @Override
  public Integration getIntegration() {
    return new MixpanelIntegration();
  }

  @Override
  public EasyJSONObject getSettings() {
    EasyJSONObject settings = new EasyJSONObject();
    settings.put("token", "89f86c4aa2ce5b74cb47eb5ec95ad1f9");
    settings.put("people", true);
    return settings;
  }

  @Test
  public void testAlias() {

    reachReadyState();

    int random = (new Random()).nextInt();

    String anonymousId = "android_anonymous_id_" + random;
    String userId = "android_user_id_" + random;

    Props properties = new Props("revenue", 10.00);
    Traits traits = new Traits("$first_name", "Ilya " + random);
    Options options = new Options().setAnonymousId(anonymousId);

    Analytics.setAnonymousId(anonymousId);

    Log.e(Constants.TAG, "Mixpanel alias test is using session_id: " +
        anonymousId + ", and user_id: " + userId);

    integration.track(new Track(null, "Anonymous Event", properties, options));

    integration.identify(new Identify(userId, traits, options));

    integration.track(new Track(userId, "Identified Event", properties, options));

    integration.flush();

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
