package com.segment.analytics;

import android.util.Log;

class Logger {
  volatile boolean loggingEnabled;

  Logger(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }

  final static String OWNER_MAIN = "Main";
  final static String OWNER_DISPATCHER = "Dispatcher";
  final static String OWNER_INTEGRATION_MANAGER = "IntegrationManager";

  final static String VERB_CREATE = "create";
  final static String VERB_DISPATCH = "dispatch";
  final static String VERB_FLUSH = "flush";
  final static String VERB_SKIP = "skip";
  final static String VERB_INITIALIZE = "initialize";

  final static String TAG = "Segment";
  // [thread] [verb] [id] {[extras]}
  final static String FORMAT = "%1$-20s %2$-12s %3$-36s {%4$s}";
  final static String EMPTY = "";

  /** Call only if loggingEnabled is true */
  void debug(String owner, String verb, String id, String extras) {
    Log.d(TAG, String.format(FORMAT, owner, verb, id == null ? EMPTY : id,
        extras == null ? EMPTY : extras));
  }

  /** Call only if loggingEnabled is true */
  void error(String owner, String verb, String id, Throwable throwable, String extras) {
    Log.e(TAG, String.format(FORMAT, owner, verb + " (error)", id == null ? EMPTY : id,
            extras == null ? EMPTY : extras) + Log.getStackTraceString(throwable)
    );
  }
}
