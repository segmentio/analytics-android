/**
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
package com.segment.analytics;

import android.annotation.SuppressLint;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import java.io.IOException;

/**
 * A {@link WearableListenerService} that listens for analytics events from a wear device.
 *
 * <p>Clients may subclass this and override {@link #getAnalytics()} to provide custom instances of
 * {@link Analytics} client. Ideally, it should be the same instance as the client you're using to
 * track events on the host Android device.
 */
@SuppressLint("Registered")
public class PhoneAnalyticsListenerService extends WearableListenerService {

  final Cartographer cartographer = Cartographer.INSTANCE;

  @Override
  public void onMessageReceived(MessageEvent messageEvent) {
    super.onMessageReceived(messageEvent);

    if (WearAnalytics.ANALYTICS_PATH.equals(messageEvent.getPath())) {
      WearPayload wearPayload;
      try {
        wearPayload = new WearPayload(cartographer.fromJson(new String(messageEvent.getData())));
      } catch (IOException e) {
        getAnalytics()
            .getLogger()
            .error(e, "Could not deserialize event %s", new String(messageEvent.getData()));
        return;
      }
      switch (wearPayload.type()) {
        case track:
          WearTrackPayload wearTrackPayload = wearPayload.payload(WearTrackPayload.class);
          getAnalytics().track(wearTrackPayload.getEvent(), wearTrackPayload.getProperties(), null);
          break;
        case screen:
          WearScreenPayload wearScreenPayload = wearPayload.payload(WearScreenPayload.class);
          getAnalytics()
              .screen(
                  wearScreenPayload.getName(),
                  wearScreenPayload.getCategory(),
                  wearScreenPayload.getProperties());
          break;
        default:
          throw new UnsupportedOperationException("Only track/screen calls may be sent from Wear.");
      }
    }
  }

  public Analytics getAnalytics() {
    return Analytics.with(this);
  }
}
