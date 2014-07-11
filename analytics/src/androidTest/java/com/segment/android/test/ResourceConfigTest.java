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

package com.segment.android.test;

import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityTestCase;
import com.segment.android.Analytics;
import com.segment.android.Config;
import com.segment.android.ResourceConfig;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class ResourceConfigTest extends ActivityTestCase {

  @Test
  public void testSecret() {
    Context context = getInstrumentation().getContext();
    Resources resources = context.getResources();
    String writeKey = ResourceConfig.getWriteKey(context);
    assertThat(resources.getString(R.string.analytics_secret)).isEqualTo(writeKey);
  }

  @Test
  public void testOptions() {
    Context context = getInstrumentation().getContext();
    Config options = ResourceConfig.getOptions(context);
    testOptions(context, options);
  }

  @Test
  public void testInitialization() {
    Context context = getInstrumentation().getContext();
    if (Analytics.isInitialized()) Analytics.close();
    Analytics.initialize(context);
    Config options = Analytics.getOptions();
    testOptions(context, options);
    Analytics.close();
  }

  private void testOptions(Context context, Config options) {

    Resources resources = context.getResources();

    assertThat(resources.getInteger(R.integer.analytics_flush_after)).isEqualTo(
        options.getFlushAfter());
    assertThat(resources.getInteger(R.integer.analytics_flush_at)).isEqualTo(options.getFlushAt());
    assertThat(resources.getInteger(R.integer.analytics_max_queue_size)).isEqualTo(
        options.getMaxQueueSize());
    assertThat(resources.getInteger(R.integer.analytics_settings_cache_expiry)).isEqualTo(
        options.getSettingsCacheExpiry());
    assertThat(resources.getString(R.string.analytics_host)).isEqualTo(options.getHost());
    assertThat(Boolean.parseBoolean(resources.getString(R.string.analytics_debug))).isEqualTo(
        options.isDebug());
  }
}
