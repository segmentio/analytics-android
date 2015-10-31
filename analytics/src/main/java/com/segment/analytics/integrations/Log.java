package com.segment.analytics.integrations;

import com.segment.analytics.Analytics.LogLevel;

import static com.segment.analytics.Analytics.LogLevel.DEBUG;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;

/** An abstraction for logging messages. */
public final class Log {
  private final static String DEFAULT_TAG = "Analytics";
  public final LogLevel logLevel;

  public Log(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

  public void verbose(String format, Object... extra) {
    if (shouldLog(VERBOSE)) {
      android.util.Log.v(DEFAULT_TAG, String.format(format, extra));
    }
  }

  public void info(String format, Object... extra) {
    if (shouldLog(INFO)) {
      android.util.Log.i(DEFAULT_TAG, String.format(format, extra));
    }
  }

  public void debug(String format, Object... extra) {
    if (shouldLog(DEBUG)) {
      android.util.Log.d(DEFAULT_TAG, String.format(format, extra));
    }
  }

  public void error(Throwable error, String format, Object... extra) {
    if (shouldLog(INFO)) {
      android.util.Log.e(DEFAULT_TAG, String.format(format, extra), error);
    }
  }

  protected boolean shouldLog(LogLevel level) {
    return logLevel.ordinal() >= level.ordinal();
  }
}
