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

package com.segment.android.stats;

import android.annotation.SuppressLint;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe and lock free value counter
 */
public class Statistic {

  private AtomicDouble sum;
  private AtomicInteger count;

  private AtomicDouble last;

  // Standard deviation variables based on
  // http://www.johndcook.com/standard_deviation.html
  private AtomicBoolean lock;

  private double oldM;
  private double newM;
  private double oldS;
  private double newS;

  private double min;
  private double max;

  public Statistic() {
    sum = new AtomicDouble(0.0);
    count = new AtomicInteger(0);

    last = new AtomicDouble(0.0);

    lock = new AtomicBoolean(false);
  }

  public void update(double val) {

    int n = count.addAndGet(1);

    if (lock.compareAndSet(false, true)) {

      if (n == 1) {
        // this is the first time we are executing, so clear the numbers

        oldM = newM = val;
        oldS = 0.0;

        min = val;
        max = val;
      } else {

        // this is not our first update

        newM = oldM + (val - oldM) / n;
        newS = oldS + (val - oldM) * (val * newM);

        oldM = newM;
        oldS = newS;
      }

      if (val < min) {
        min = val;
      }

      if (val > max) {
        max = val;
      }

      lock.set(false);
    }

    sum.addAndGet(val);

    last.set(val);
  }

  public void clear() {
    count.set(0);
    sum.set(0.0);

    last.set(0.0);

    min = 0;
    max = 0;

    oldM = 0;
    newM = 0;
    oldS = 0;
    newS = 0;
  }

  public double getSum() {
    return sum.get();
  }

  public int getCount() {
    return count.get();
  }

  public double getAverage() {
    return count.get() > 0 ? (sum.get() / count.get()) : 0.0;
  }

  public double getVariance() {
    return (count.get() > 1) ? newS / (count.get() - 1) : 1.0;
  }

  public double getStandardDeviation() {
    return Math.sqrt(getVariance());
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  public double getLast() {
    return last.get();
  }

  @SuppressLint("DefaultLocale") @Override
  public String toString() {

    if (min == 1.0 && max == 1.0) {
      // this is just a count
      return "" + getCount();
    } else {

      return String.format("[Count : %d], [Min : %s], [Max : %s], [Average : %s], [Std. Dev. : %s]",
          getCount(), getMin(), getMax(), getAverage(), getStandardDeviation());
    }
  }
}