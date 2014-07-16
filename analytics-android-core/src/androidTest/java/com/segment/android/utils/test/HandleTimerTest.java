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

package com.segment.android.utils.test;

import android.test.AndroidTestCase;
import com.segment.android.utils.HandlerTimer;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class HandleTimerTest extends AndroidTestCase {

  @Test
  public void testTimer() {

    int frequency = 200;
    int expected = 10;

    int timeToWait = frequency * expected + (frequency / 4);

    final AtomicInteger counter = new AtomicInteger();

    HandlerTimer timer = new HandlerTimer(frequency, new Runnable() {
      @Override
      public void run() {
        counter.addAndGet(1);
      }
    });

    // start the timer
    timer.start();

    try {
      // wait for expected * frequency seconds
      Thread.sleep(timeToWait);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    int got1 = counter.get();

    // assume that we get expected counts
    assertThat(got1).isEqualTo(expected);

    // stop the timer
    timer.quit();

    // check again

    try {
      Thread.sleep(frequency * 2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    int got2 = counter.get();

    // assume that we didn't grow since we stopped the timer
    assertThat(got2).isEqualTo(got1);
  }
}