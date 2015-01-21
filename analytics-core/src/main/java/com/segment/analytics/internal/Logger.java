package com.segment.analytics.internal;

import android.util.Log;

public class Logger {
  private final static String TAG = "Segment";
  // [thread] [verb] [id] {[extras]}
  private final static String DEBUG_FORMAT = "%1$-20s %2$-12s %3$-36s {%4$s}";
  private final static String ERROR_FORMAT = "%1$-20s %2$-12s %3$-36s {%4$s}\n%5$s";

  public final static String OWNER_MAIN = "Main";
  public final static String OWNER_SEGMENT = "Segment";
  public final static String OWNER_INTEGRATION_MANAGER = "IntegrationManager";
  public final static String VERB_CREATE = "create";
  public final static String VERB_DISPATCH = "dispatch";
  public final static String VERB_ENQUEUE = "enqueue";
  public final static String VERB_FLUSH = "flush";
  public final static String VERB_SKIP = "skip";
  public final static String VERB_INITIALIZE = "initialize";

  final boolean loggingEnabled;

  private static String safeFormat(String format, Object... extras) {
    return format == null ? null : String.format(format, extras);
  }

  public Logger(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }

  public void debug(String owner, String verb, String id, String format, Object... extras) {
    if (loggingEnabled) {
      Log.d(TAG, String.format(DEBUG_FORMAT, owner, verb, id, safeFormat(format, extras)));
    }
  }

  public void error(String owner, String verb, String id, Throwable throwable, String format,
      Object... extras) {
    if (loggingEnabled) {
      Log.e(TAG,
          String.format(ERROR_FORMAT, owner, "ERROR: " + verb, id, safeFormat(format, extras),
              Log.getStackTraceString(throwable)));
    }
  }

  public void print(Throwable throwable, String format, Object... extras) {
    if (loggingEnabled) {
      Log.e(TAG, Log.getStackTraceString(throwable));
      Log.e(TAG, String.format(format, extras));
    }
  }

  public void print(String format, Object... extras) {
    if (loggingEnabled) {
      Log.d(TAG, String.format(format, extras));
    }
  }
}
