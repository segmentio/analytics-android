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

import com.segment.android.Logger;

/**
 * A utility class to time an operation, and log it
 * after its ended.
 */
public class Stopwatch {

  private String msg;
  private long start;
  private long end;

  /**
   * Create and start a new timed operation.
   *
   * @param msg Message representing the operation
   */
  public Stopwatch(String msg) {
    this.msg = msg;
    start();
  }

  /**
   * Start the operation
   */
  public void start() {
    start = System.currentTimeMillis();
  }

  /**
   * End the operation and log its result
   */
  public void end() {
    end = System.currentTimeMillis();

    Logger.d("[Stopwatch] %s finished in %d milliseconds.", msg, duration());
  }

  /**
   * Returns the millisecond duration of this operation
   */
  public long duration() {
    return end - start;
  }
}
