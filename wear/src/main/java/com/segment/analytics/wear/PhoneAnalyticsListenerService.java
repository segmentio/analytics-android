/*
 * Copyright 2014 Prateek Srivastava
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.segment.analytics.wear;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.segment.analytics.Analytics;
import com.segment.analytics.internal.Logger;
import com.segment.analytics.wear.model.WearPayload;
import com.segment.analytics.wear.model.WearScreenPayload;
import com.segment.analytics.wear.model.WearTrackPayload;

public class PhoneAnalyticsListenerService extends WearableListenerService {

  @Override public void onMessageReceived(MessageEvent messageEvent) {
    super.onMessageReceived(messageEvent);

    if (messageEvent.getPath().contains("analytics")) {
      WearPayload wearPayload = new WearPayload(new String(messageEvent.getData()));

      switch (wearPayload.type()) {
        case track:
          WearTrackPayload wearTrackPayload = wearPayload.payload(WearTrackPayload.class);
          getAnalytics().track(wearTrackPayload.getEvent(), wearTrackPayload.getProperties(), null);
          break;
        case screen:
          WearScreenPayload wearScreenPayload = wearPayload.payload(WearScreenPayload.class);
          getAnalytics().screen(wearScreenPayload.getName(), wearScreenPayload.getCategory(),
              wearScreenPayload.getProperties());
          break;
        default:
          Logger.d("Only screen and track events are supported for Wear Analytics.");
      }
    }
  }

  public Analytics getAnalytics() {
    return Analytics.with(this);
  }
}
