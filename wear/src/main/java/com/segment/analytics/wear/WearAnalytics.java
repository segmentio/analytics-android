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

import android.content.Context;
import android.content.Intent;
import com.segment.analytics.Properties;
import com.segment.analytics.internal.payload.BasePayload;
import com.segment.analytics.wear.model.WearPayload;
import com.segment.analytics.wear.model.WearScreenPayload;
import com.segment.analytics.wear.model.WearTrackPayload;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

public class WearAnalytics {
  final Context context;

  WearAnalytics(Context context) {
    this.context = context;
  }

  static WearAnalytics singleton;

  public static WearAnalytics with(Context context) {
    if (singleton == null) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      synchronized (WearAnalytics.class) {
        if (singleton == null) {
          singleton = new WearAnalytics(context);
        }
      }
    }
    return singleton;
  }

  /**
   * The track method is how you record any actions your users perform. Each action is known by a
   * name, like 'Purchased a T-Shirt'. You can also record properties specific to those actions.
   * For
   * example a 'Purchased a Shirt' event might have properties like revenue or size.
   *
   * @param event Name of the event. Must not be null or empty.
   * @param properties {@link Properties} to add extra information to this call
   * @throws IllegalArgumentException if event name is null or an empty string
   * @see <a href="https://segment.io/docs/tracking-api/track/">Track Documentation</a>
   */
  public void track(String event, Properties properties) {
    if (isNullOrEmpty(event)) {
      throw new IllegalArgumentException("event must not be null or empty.");
    }

    if (properties == null) {
      properties = new Properties();
    }

    Intent intent = new Intent("analytics", null, context, WearAnalyticsService.class);
    intent.putExtra("payload", new WearPayload(BasePayload.Type.track,
        new WearTrackPayload(event, properties)).toString());
    context.startService(intent);
  }

  /**
   * The screen methods let your record whenever a user sees a screen of your mobile app, and
   * attach a name, category or properties to the screen.
   * <p/>
   * Either category or name must be provided.
   *
   * @param category A category to describe the screen
   * @param name A name for the screen
   * @param properties {@link Properties} to add extra information to this call
   * @see <a href="http://segment.io/docs/tracking-api/page-and-screen/">Screen Documentation</a>
   */
  public void screen(String category, String name, Properties properties) {
    if (isNullOrEmpty(category) && isNullOrEmpty(name)) {
      throw new IllegalArgumentException("either category or name must be provided.");
    }

    if (properties == null) {
      properties = new Properties();
    }

    Intent intent = new Intent("analytics", null, context, WearAnalyticsService.class);
    intent.putExtra("payload", new WearPayload(BasePayload.Type.track,
        new WearScreenPayload(category, name, properties)).toString());
    context.startService(intent);
  }
}
