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
package com.segment.analytics.sample;

import android.app.Application;
import android.util.Log;
import com.segment.analytics.Analytics;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class SampleApp extends Application {

  // https://segment.com/segment-engineering/sources/android-test/settings/keys
  private static final String ANALYTICS_WRITE_KEY = "5m6gbdgho6";

  @Override
  public void onCreate() {
    super.onCreate();

    CalligraphyConfig.initDefault(
        new CalligraphyConfig.Builder()
            .setDefaultFontPath("fonts/CircularStd-Book.otf")
            .setFontAttrId(R.attr.fontPath)
            .build());

    // Initialize a new instance of the Analytics client.
    Analytics.Builder builder =
        new Analytics.Builder(this, ANALYTICS_WRITE_KEY)
            .trackApplicationLifecycleEvents()
            .trackAttributionInformation()
            .recordScreenViews();

    // Set the initialized instance as a globally accessible instance.
    Analytics.setSingletonInstance(builder.build());

    // Now anytime you call Analytics.with, the custom instance will be returned.
    Analytics analytics = Analytics.with(this);

    // If you need to know when integrations have been initialized, use the onIntegrationReady
    // listener.
    analytics.onIntegrationReady(
        "Segment.io",
        new Analytics.Callback() {
          @Override
          public void onReady(Object instance) {
            Log.d("Segment Sample", "Segment integration ready.");
          }
        });
  }
}
