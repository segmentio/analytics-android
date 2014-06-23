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

package com.segment.android;

import android.util.Log;

public class Logger {

  public static final String TAG = Constants.TAG;

  private static boolean log;

  /**
   * Set whether this logger logs (true to log)
   */
  public static void setLog(boolean log) {
    Logger.log = log;
  }

  /**
   * Get whether this logger logs (true to log)
   */
  public static boolean isLogging() {
    return Logger.log;
  }

  public static void v(String msg) {
    if (log) Log.v(TAG, msg);
  }

  public static void v(String msg, Throwable tr) {
    if (log) Log.v(TAG, msg, tr);
  }

  public static void d(String msg) {
    if (log) Log.d(TAG, msg);
  }

  public static void d(String msg, Throwable tr) {
    if (log) Log.d(TAG, msg, tr);
  }

  public static void i(String msg) {
    if (log) Log.i(TAG, msg);
  }

  public static void i(String msg, Throwable tr) {
    if (log) Log.i(TAG, msg, tr);
  }

  public static void w(String msg) {
    if (log) Log.w(TAG, msg);
  }

  public static void w(String msg, Throwable tr) {
    if (log) Log.w(TAG, msg, tr);
  }

  public static void e(String msg) {
    if (log) Log.e(TAG, msg);
  }

  public static void e(String msg, Throwable tr) {
    if (log) Log.e(TAG, msg, tr);
  }
}
