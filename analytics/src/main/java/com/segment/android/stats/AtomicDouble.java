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

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Double.doubleToLongBits;

/**
 * A thread-safe and lock-free double implementation
 * based on this stack overflow answer:
 * http://stackoverflow.com/questions/5505460/java-is-there-no-atomicfloat-or-atomicdouble
 */
class AtomicDouble extends Number {

  private static final long serialVersionUID = -2480549991498013056L;
  private AtomicLong bits;

  public AtomicDouble() {
    this(0);
  }

  public AtomicDouble(double initialValue) {
    bits = new AtomicLong(doubleToLongBits(initialValue));
  }

  public final boolean compareAndSet(double expect, double update) {
    return bits.compareAndSet(doubleToLongBits(expect), doubleToLongBits(update));
  }

  public final void set(double newValue) {
    bits.set(doubleToLongBits(newValue));
  }

  public final double get() {

    return Double.longBitsToDouble(bits.get());
  }

  public float floatValue() {
    return (float) get();
  }

  public final double addAndGet(double newValue) {
    return Double.longBitsToDouble(bits.addAndGet(doubleToLongBits(newValue)));
  }

  public final double getAndSet(double newValue) {
    return Double.longBitsToDouble(bits.getAndSet(doubleToLongBits(newValue)));
  }

  public final boolean weakCompareAndSet(float expect, float update) {
    return bits.weakCompareAndSet(doubleToLongBits(expect), doubleToLongBits(update));
  }

  public double doubleValue() {
    return (double) floatValue();
  }

  public int intValue() {
    return (int) get();
  }

  public long longValue() {
    return (long) get();
  }
}