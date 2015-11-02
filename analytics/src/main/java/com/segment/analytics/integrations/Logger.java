package com.segment.analytics.integrations;

import android.util.Log;
import com.segment.analytics.Analytics.LogLevel;

import static com.segment.analytics.Analytics.LogLevel.DEBUG;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;

/** An abstraction for logging messages. */
public final class Logger {
  private final static String DEFAULT_TAG = "Analytics";
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
   * Returns a new {@link Logger} with the same {@code level} as this one and the given {@code
   * tag}.
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
