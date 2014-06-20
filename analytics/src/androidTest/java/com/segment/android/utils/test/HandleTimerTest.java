package com.segment.android.utils.test;

import android.test.AndroidTestCase;
import com.segment.android.utils.HandlerTimer;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.Assert;
import org.junit.Test;

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
    Assert.assertEquals(expected, got1);

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
    Assert.assertEquals(got1, got2);
  }
}