package com.segment.android.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.android.Segment;
import com.segment.android.StatsSnapshot;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class Stats {
  private static final int IDENTIFY = 1;
  private static final int GROUP = 2;
  private static final int TRACK = 3;
  private static final int SCREEN = 4;
  private static final int ALIAS = 5;
  private static final int FLUSH = 6;
  private static final int INTEGRATION_OPERATION = 7;

  private static final String STATS_THREAD_NAME = Utils.THREAD_PREFIX + "Stats";

  final HandlerThread statsThread;
  final Handler handler;

  long identifyCount;
  long groupCount;
  long trackCount;
  long screenCount;
  long aliasCount;
  long flushCount; // number of times we flushed to server
  long flushEventCount; // number of events we flush to the server
  long integrationOperationDuration;
  long integrationOperationTime;

  public Stats() {
    statsThread = new HandlerThread(STATS_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    statsThread.start();
    handler = new StatsHandler(statsThread.getLooper(), this);
  }

  public void shutdown() {
    statsThread.quit();
  }

  public void dispatchIdentify() {
    handler.sendMessage(handler.obtainMessage(IDENTIFY));
  }

  public void dispatchGroup() {
    handler.sendMessage(handler.obtainMessage(GROUP));
  }

  public void dispatchTrack() {
    handler.sendMessage(handler.obtainMessage(TRACK));
  }

  public void dispatchScreen() {
    handler.sendMessage(handler.obtainMessage(SCREEN));
  }

  public void dispatchAlias() {
    handler.sendMessage(handler.obtainMessage(ALIAS));
  }

  public void dispatchFlush(int count) {
    handler.sendMessage(handler.obtainMessage(FLUSH, count, 0));
  }

  public void dispatchIntegrationOperation(long duration) {
    handler.sendMessage(handler.obtainMessage(INTEGRATION_OPERATION, duration));
  }

  void performIdentify() {
    identifyCount++;
  }

  void performGroup() {
    groupCount++;
  }

  void performTrack() {
    trackCount++;
  }

  void performScreen() {
    screenCount++;
  }

  void performAlias() {
    aliasCount++;
  }

  void performIntegrationOperation(long duration) {
    integrationOperationDuration++;
    integrationOperationTime += duration;
  }

  void performFlush(int count) {
    flushCount++;
    flushEventCount += count;
  }

  private static class StatsHandler extends Handler {
    private final Stats stats;

    public StatsHandler(Looper looper, Stats stats) {
      super(looper);
      this.stats = stats;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case IDENTIFY:
          stats.performIdentify();
          break;
        case GROUP:
          stats.performGroup();
          break;
        case TRACK:
          stats.performTrack();
          break;
        case SCREEN:
          stats.performScreen();
          break;
        case ALIAS:
          stats.performAlias();
          break;
        case FLUSH:
          stats.performFlush(msg.arg1);
          break;
        case INTEGRATION_OPERATION:
          stats.performIntegrationOperation((Long) msg.obj);
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
    return new StatsSnapshot(System.currentTimeMillis(), identifyCount, groupCount, trackCount,
        screenCount, flushCount, flushEventCount, integrationOperationDuration,
        integrationOperationTime);
  }
}
