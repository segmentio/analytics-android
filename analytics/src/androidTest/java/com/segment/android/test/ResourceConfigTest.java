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
import org.junit.Assert;
import org.junit.Test;

public class ResourceConfigTest extends ActivityTestCase {

  @Test
  public void testSecret() {
    Context context = getInstrumentation().getContext();
    Resources resources = context.getResources();
    String writeKey = ResourceConfig.getWriteKey(context);
    Assert.assertEquals(resources.getString(R.string.analytics_secret), writeKey);
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

    Assert.assertEquals(resources.getInteger(R.integer.analytics_flush_after),
        options.getFlushAfter());
    Assert.assertEquals(resources.getInteger(R.integer.analytics_flush_at), options.getFlushAt());
    Assert.assertEquals(resources.getInteger(R.integer.analytics_max_queue_size),
        options.getMaxQueueSize());
    Assert.assertEquals(resources.getInteger(R.integer.analytics_settings_cache_expiry),
        options.getSettingsCacheExpiry());

    Assert.assertEquals(resources.getString(R.string.analytics_host), options.getHost());
    Assert.assertEquals(Boolean.parseBoolean(resources.getString(R.string.analytics_debug)),
        options.isDebug());
  }
}
