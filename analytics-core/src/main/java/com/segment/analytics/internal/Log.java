package com.segment.analytics.internal;

import com.segment.analytics.Analytics;

import static com.segment.analytics.Analytics.LogLevel.DEBUG;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;

public final class Log {
  private final static String DEFAULT_TAG = "Analytics";
  public final Analytics.LogLevel logLevel;
  private final String tag;

  private Log(Analytics.LogLevel logLevel, String tag) {
    this.logLevel = logLevel;
    this.tag = tag;
  }

  public void verbose(String format, Object... extra) {
    if (logLevel.ordinal() >= VERBOSE.ordinal()) {
      android.util.Log.v(tag, String.format(format, extra));
    }
  }

  public void info(String format, Object... extra) {
    if (logLevel.ordinal() >= INFO.ordinal()) {
      android.util.Log.i(tag, String.format(format, extra));
    }
  }

  public void debug(String format, Object... extra) {
    if (logLevel.ordinal() >= DEBUG.ordinal()) {
      android.util.Log.d(tag, String.format(format, extra));
    }
  }

  public void error(Throwable error, String format, Object... extra) {
    if (logLevel.ordinal() >= INFO.ordinal()) {
      android.util.Log.e(tag, String.format(format, extra), error);
    }
  }

  public Log newLogger(String tag) {
    return new Log(logLevel, DEFAULT_TAG + "-" + tag);
  }

  public static Log with(Analytics.LogLevel logLevel) {
    return new Log(logLevel, DEFAULT_TAG);
  }
}
