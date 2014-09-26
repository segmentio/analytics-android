package com.segment.analytics;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;

class Stats {
  private static final String STATS_THREAD_NAME = Utils.THREAD_PREFIX + "Stats";
  private static final int FLUSH = 1;
  private static final int INTEGRATION_OPERATION = 2;

  final HandlerThread statsThread;
  final Handler handler;

  long flushCount; // number of times we flushed to server
  long flushEventCount; // number of events we flushed to server
  long integrationOperationCount; // number of events sent to integrations
  long integrationOperationTime; // total time to run integrations

  Stats() {
    statsThread = new HandlerThread(STATS_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    statsThread.start();
    handler = new StatsHandler(statsThread.getLooper(), this);
  }

  void shutdown() {
    quitThread(statsThread);
  }

  void dispatchFlush(int count) {
    handler.sendMessage(handler.obtainMessage(FLUSH, count, 0));
  }

  void dispatchIntegrationOperation(long duration) {
    handler.sendMessage(handler.obtainMessage(INTEGRATION_OPERATION, duration));
  }

  void performIntegrationOperation(long duration) {
    integrationOperationCount++;
    integrationOperationTime += duration;
  }

  void performFlush(int count) {
    flushCount++;
    flushEventCount += count;
  }

  private static class StatsHandler extends Handler {
    private final Stats stats;

    StatsHandler(Looper looper, Stats stats) {
      super(looper);
      this.stats = stats;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case FLUSH:
          stats.performFlush(msg.arg1);
          break;
        case INTEGRATION_OPERATION:
          stats.performIntegrationOperation((Long) msg.obj);
          break;
        default:
          panic("Unhandled stats message." + msg.what);
      }
    }
  }

  StatsSnapshot createSnapshot() {
    return new StatsSnapshot(System.currentTimeMillis(), flushCount, flushEventCount,
        integrationOperationCount, integrationOperationTime);
  }
}
