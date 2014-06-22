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

package com.segment.android.flush.test;

import com.segment.android.Analytics;
import com.segment.android.stats.AnalyticsStatistics;
import com.segment.android.test.BaseTest;
import com.segment.android.test.TestCases;
import org.junit.Assert;
import org.junit.Test;

public class FlushTests extends BaseTest {

  @Test
  public void testEmptyFlush() {

    AnalyticsStatistics stats = Analytics.getStatistics();

    int flushAttempts = stats.getFlushAttempts().getCount();
    int requests = stats.getRequestTime().getCount();

    Analytics.flush(false);
    Analytics.flush(false);
    Analytics.flush(false);

    flushAttempts += 3;

    Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());
    Assert.assertEquals(0, requests);
  }

  @Test
  public void testFlushAtTrigger() {

    int flushAt = Analytics.getOptions().getFlushAt();

    AnalyticsStatistics stats = Analytics.getStatistics();

    int flushAttempts = stats.getFlushAttempts().getCount();

    for (int i = 0; i < flushAt; i += 1) {
      Analytics.enqueue(TestCases.random());
    }

    // we expect that the flushing happened here
    flushAttempts += 1;

    // we want to wait until the flush actually happens
    Analytics.flush(false);
    flushAttempts += 1;

    Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());
  }

  @Test
  public void testFlushAfterTrigger() {

    int flushAfter = Analytics.getOptions().getFlushAfter();

    AnalyticsStatistics stats = Analytics.getStatistics();

    int flushAttempts = stats.getFlushAttempts().getCount();
    int requests = stats.getRequestTime().getCount();

    Analytics.enqueue(TestCases.random());

    try {
      Thread.sleep(flushAfter + 250);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // the flush after timer should have triggered a flush here
    flushAttempts += 1;
    requests += 1;

    // we want to wait until the flush actually happens
    Analytics.flush(false);
    flushAttempts += 1;

    Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());

    Assert.assertEquals(requests, stats.getRequestTime().getCount());
  }
}
