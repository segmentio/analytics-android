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

import android.util.Log;
import com.segment.android.Analytics;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Batch;
import com.segment.android.request.BasicRequester;
import com.segment.android.test.BaseTest;
import com.segment.android.test.TestCases;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.junit.Test;

public class DuplicateTest extends BaseTest {

  public static final String TAG = "DuplicateTest";

  public class DuplicateCountingRequester extends BasicRequester {

    private final Set<String> requestIds = new HashSet<String>();
    private final AtomicInteger duplicates = new AtomicInteger();

    @Override
    public HttpResponse send(Batch batch) {
      for (BasePayload payload : batch.getBatch()) {
        if (requestIds.contains(payload.getMessageId())) {
          // we've already sent this action
          duplicates.addAndGet(1);
          Log.w(TAG, "Detected duplicate " + payload.getMessageId());
        } else {
          // we haven't seen this action, lets remember it
          requestIds.add(payload.getMessageId());
        }
      }
      return super.send(batch);
    }

    public int countDuplicates() {
      return duplicates.get();
    }
  }

  @Test
  public void testForDuplicates() throws InterruptedException {
    int inserts = 200;
    int flushesPerInsert = 2;

    // set a custom counting duplicate requester
    DuplicateCountingRequester requester = new DuplicateCountingRequester();
    Analytics.setRequester(requester);

    for (int i = 0; i < inserts; i += 1) {
      // enqueue a random action
      Analytics.enqueue(TestCases.random());
      Log.i(TAG, "Enqueued message " + (i + 1) + " / " + inserts);

      // trigger a series of flushes for every insert
      for (int j = 0; j < flushesPerInsert; j += 1) {
        Analytics.flush(true);
      }

      // and wait for some stuff to happen
      Thread.sleep(30);
    }

    Log.i(TAG, "Final blocking flush ..");
    // now let's block and wait for everything to send
    Analytics.flush(false);
    Log.i(TAG, "Detected " + requester.countDuplicates() + " duplicates.");

    // check that we haven't seen any duplicates
    Assert.assertEquals(0, requester.countDuplicates());
  }
}