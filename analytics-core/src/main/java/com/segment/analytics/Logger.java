package com.segment.analytics;

import android.util.Log;

class Logger {
  final static String TAG = "Segment";
  // [thread] [verb] [id] {[extras]}
  final static String DEBUG_FORMAT = "%1$-20s %2$-12s %3$-36s {%4$s}";
  final static String ERROR_FORMAT = "%1$-20s %2$-12s %3$-36s {%4$s}\n%5$s";

  final static String OWNER_MAIN = "Main";
  final static String OWNER_SEGMENT = "Segment";
  final static String OWNER_INTEGRATION_MANAGER = "IntegrationManager";
  final static String VERB_CREATE = "create";
  final static String VERB_DISPATCH = "dispatch";
  final static String VERB_ENQUEUE = "enqueue";
  final static String VERB_FLUSH = "flush";
  final static String VERB_SKIP = "skip";
  final static String VERB_INITIALIZE = "initialize";

  final boolean loggingEnabled;

  private static String safeFormat(String format, Object... extras) {
    return format == null ? null : String.format(format, extras);
  }

  public Logger(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }

  void debug(String owner, String verb, String id, String format, Object... extras) {
    if (loggingEnabled) {
      Log.d(TAG, String.format(DEBUG_FORMAT, owner, verb, id, safeFormat(format, extras)));
    }
  }

  void error(String owner, String verb, String id, Throwable throwable, String format,
      Object... extras) {
    if (loggingEnabled) {
      Log.e(TAG,
          String.format(ERROR_FORMAT, owner, "ERROR: " + verb, id, safeFormat(format, extras),
              Log.getStackTraceString(throwable)));
    }
  }

  void print(Throwable throwable, String format, Object... extras) {
    if (loggingEnabled) {
      if (throwable != null) Log.e(TAG, Log.getStackTraceString(throwable));
      Log.e(TAG, String.format(format, extras));
    }
  }
}
