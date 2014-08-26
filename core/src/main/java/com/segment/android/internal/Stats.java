package com.segment.android.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.android.Segment;
import com.segment.android.StatsSnapshot;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class Stats {
  private static final int EVENT = 0;
  private static final int FLUSH = 1;
  private static final int INTEGRATION_OPERATION = 3;
  private static final int REPLAY = 4;

  private static final String STATS_THREAD_NAME = Utils.THREAD_PREFIX + "Stats";

  final HandlerThread statsThread;
  final Handler handler;

  long eventCount;
  long flushCount;
  long flushEventCount;
  long integrationOperationCount;
  long integrationOperationTime;
  int replayCount;

  public Stats() {
    statsThread = new HandlerThread(STATS_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    statsThread.start();
    handler = new StatsHandler(statsThread.getLooper(), this);
  }

  public void shutdown() {
    statsThread.quit();
  }

  public void dispatchEvent() {
    handler.sendMessage(handler.obtainMessage(EVENT));
  }

  public void dispatchIntegrationOperation(long duration) {
    handler.sendMessage(handler.obtainMessage(INTEGRATION_OPERATION, duration));
  }

  public void dispatchFlush(int count) {
    handler.sendMessage(handler.obtainMessage(FLUSH, count, 0));
  }

  public void dispatchReplay() {
    handler.sendMessage(handler.obtainMessage(REPLAY));
  }

  void performEvent() {
    eventCount++;
  }

  void performIntegrationOperation(long duration) {
    integrationOperationCount++;
    integrationOperationTime += duration;
  }

  void performFlush(int count) {
    flushCount++;
    flushEventCount += count;
  }

  void performReplay() {
    replayCount++;
  }

  private static class StatsHandler extends Handler {
    private final Stats stats;

    public StatsHandler(Looper looper, Stats stats) {
      super(looper);
      this.stats = stats;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case EVENT:
          stats.performEvent();
          break;
        case FLUSH:
          stats.performFlush(msg.arg1);
          break;
        case INTEGRATION_OPERATION:
          stats.performIntegrationOperation((Long) msg.obj);
          break;
        case REPLAY:
          stats.performReplay();
          break;
        default:
          Segment.HANDLER.post(new Runnable() {
            @Override public void run() {
              throw new AssertionError("Unhandled stats message." + msg.what);
            }
          });
      }
    }
  }

  public StatsSnapshot createSnapshot() {
    return new StatsSnapshot(System.currentTimeMillis(), eventCount, flushCount, flushEventCount,
        integrationOperationCount, integrationOperationTime, replayCount);
  }
}
