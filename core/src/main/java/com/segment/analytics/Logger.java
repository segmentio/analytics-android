package com.segment.analytics;

import android.util.Log;

public class Logger {
  volatile boolean loggingEnabled;

  public Logger(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }

  final static String THREAD_MAIN = "Main";
  final static String THREAD_DISPATCHER = "Dispatcher";
  final static String THREAD_INTEGRATION_MANAGER = "IntegrationManager";

  final static String VERB_CREATED = "created";
  final static String VERB_DISPATCHED = "dispatched";
  final static String VERB_DISPATCHING = "dispatching";
  final static String VERB_FLUSHING = "flushing";
  final static String VERB_FLUSHED = "flushed";
  final static String VERB_SKIPPED = "skipped";
  final static String VERB_INITIALIZING = "initializing";
  final static String VERB_INITIALIZED = "initialized";

  final static String TAG = "Segment";
  // [thread] [verb] [id] [extras]
  final static String FORMAT = "%1$-20s %2$-12s %3$-36s %4$s";
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
