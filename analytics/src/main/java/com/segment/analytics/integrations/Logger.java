/**
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
package com.segment.analytics.integrations;

import static com.segment.analytics.Analytics.LogLevel.DEBUG;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;

import android.util.Log;
import com.segment.analytics.Analytics.LogLevel;

/** An abstraction for logging messages. */
public final class Logger {
  private static final String DEFAULT_TAG = "Analytics";
  public final LogLevel logLevel;
  private final String tag;

  public Logger(String tag, LogLevel logLevel) {
    this.tag = tag;
    this.logLevel = logLevel;
  }

  /** Log a verbose message. */
  public void verbose(String format, Object... extra) {
    if (shouldLog(VERBOSE)) {
      Log.v(tag, String.format(format, extra));
    }
  }

  /** Log an info message. */
  public void info(String format, Object... extra) {
    if (shouldLog(INFO)) {
      Log.i(tag, String.format(format, extra));
    }
  }

  /** Log a debug message. */
  public void debug(String format, Object... extra) {
    if (shouldLog(DEBUG)) {
      Log.d(tag, String.format(format, extra));
    }
  }

  /** Log an error message. */
  public void error(Throwable error, String format, Object... extra) {
    if (shouldLog(INFO)) {
      Log.e(tag, String.format(format, extra), error);
    }
  }

  /**
   * Returns a new {@link Logger} with the same {@code level} as this one and the given {@code tag}.
   */
  public Logger subLog(String tag) {
    return new Logger(DEFAULT_TAG + "-" + tag, logLevel);
  }

  /** Returns a new {@link Logger} with the give {@code level}. */
  public static Logger with(LogLevel level) {
    return new Logger(DEFAULT_TAG, level);
  }

  private boolean shouldLog(LogLevel level) {
    return logLevel.ordinal() >= level.ordinal();
  }
}
