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

package com.segment.android.internal.util;

import android.util.Log;

/** {@link android.util.Log} wrapper. */
public final class Logger {
  private static final String TAG_FORMAT = "[%s] %s:%s";
  private static volatile boolean log;

  private Logger() {
    throw new AssertionError("No instances");
  }

  /** Enable({@link Boolean#TRUE}) or disable({@link Boolean#FALSE}) logging. */
  public static void setLog(boolean enabled) {
    log = enabled;
  }

  /** Get whether this logger logs (true to log) */
  public static boolean isLogging() {
    return log;
  }

  public static void v(String format, Object... args) {
    println(Log.VERBOSE, format, args);
  }

  public static void v(Throwable throwable, String format, Object... args) {
    println(Log.VERBOSE, throwable, format, args);
  }

  public static void d(String format, Object... args) {
    println(Log.DEBUG, format, args);
  }

  public static void d(Throwable throwable, String format, Object... args) {
    println(Log.DEBUG, throwable, format, args);
  }

  public static void i(String format, Object... args) {
    println(Log.INFO, format, args);
  }

  public static void i(Throwable throwable, String format, Object... args) {
    println(Log.INFO, throwable, format, args);
  }

  public static void w(String format, Object... args) {
    println(Log.WARN, format, args);
  }

  public static void w(Throwable throwable, String format, Object... args) {
    println(Log.WARN, throwable, format, args);
  }

  public static void e(String format, Object... args) {
    println(Log.ERROR, format, args);
  }

  public static void e(Throwable throwable, String format, Object... args) {
    println(Log.ERROR, throwable, format, args);
  }

  /**
   * Print the log message to {@link android.util.Log} if logging is enabled, otherwise this does
   * nothing.
   */
  private static void println(int priority, String format, Object... args) {
    if (!log) return;
    final String message = String.format(format, args);
    Log.println(priority, processTag(), message);
  }

  /**
   * Print the log message to {@link android.util.Log} if logging is enabled, otherwise this does
   * nothing.
   */
  private static void println(int priority, Throwable throwable, String format, Object... args) {
    if (!log) return;
    final String message = String.format(format, args) + '\n' + Log.getStackTraceString(throwable);
    Log.println(priority, processTag(), message);
  }

  /**
   * Returns a string suitable for tagging log messages. It includes the current thread's name, and
   * where the code was called from.
   */
  private static String processTag() {
    final Thread thread = Thread.currentThread();
    final StackTraceElement trace =
        thread.getStackTrace()[6]; // skip 6 stackframes to find the location where this was called
    return String.format(TAG_FORMAT, thread.getName(), trace.getFileName(), trace.getLineNumber());
  }
}
