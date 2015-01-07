package com.segment.analytics.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.analytics.StatsSnapshot;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.internal.Utils.panic;
import static com.segment.analytics.internal.Utils.quitThread;

public class Stats {
  private static final String STATS_THREAD_NAME = Utils.THREAD_PREFIX + "Stats";

  final HandlerThread statsThread;
  final Handler handler;

  long flushCount; // number of times we flushed to server
  long flushEventCount; // number of events we flushed to server
  long integrationOperationCount; // number of events sent to integrations
  long integrationOperationTime; // total time to run integrations

  public Stats() {
    statsThread = new HandlerThread(STATS_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    statsThread.start();
    handler = new StatsHandler(statsThread.getLooper(), this);
  }

  public void shutdown() {
    quitThread(statsThread);
  }

  void dispatchFlush(int count) {
    handler.sendMessage(handler.obtainMessage(StatsHandler.REQUEST_TRACK_FLUSH, count, 0));
  }

  void dispatchIntegrationOperation(long duration) {
    handler.sendMessage(
        handler.obtainMessage(StatsHandler.REQUEST_TRACK_INTEGRATION_OPERATION, duration));
  }

  void performIntegrationOperation(long duration) {
    integrationOperationCount++;
    integrationOperationTime += duration;
  }

  void performFlush(int count) {
    flushCount++;
    flushEventCount += count;
  }

  public StatsSnapshot createSnapshot() {
    return new StatsSnapshot(System.currentTimeMillis(), flushCount, flushEventCount,
        integrationOperationCount, integrationOperationTime);
  }

  private static class StatsHandler extends Handler {
    private static final int REQUEST_TRACK_FLUSH = 1;
    private static final int REQUEST_TRACK_INTEGRATION_OPERATION = 2;
    private final Stats stats;

    StatsHandler(Looper looper, Stats stats) {
      super(looper);
      this.stats = stats;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_TRACK_FLUSH:
          stats.performFlush(msg.arg1);
          break;
        case REQUEST_TRACK_INTEGRATION_OPERATION:
          stats.performIntegrationOperation((Long) msg.obj);
          break;
        default:
          panic("Unhandled stats message." + msg.what);
      }
    }
  }
}
