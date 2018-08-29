/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import com.segment.analytics.internal.Private;
import com.segment.analytics.internal.Utils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Stats {

  private static final String STATS_THREAD_NAME = Utils.THREAD_PREFIX + "Stats";

  final HandlerThread statsThread;
  final StatsHandler handler;

  long flushCount;
  long flushEventCount;
  long integrationOperationCount;
  long integrationOperationDuration;
  Map<String, Long> integrationOperationDurationByIntegration = new HashMap<>();

  Stats() {
    statsThread = new HandlerThread(STATS_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    statsThread.start();
    handler = new StatsHandler(statsThread.getLooper(), this);
  }

  void shutdown() {
    statsThread.quit();
  }

  void dispatchFlush(int eventCount) {
    handler.sendMessage(
        handler //
            .obtainMessage(StatsHandler.TRACK_FLUSH, eventCount, 0));
  }

  void performFlush(int eventCount) {
    flushCount++;
    flushEventCount += eventCount;
  }

  void dispatchIntegrationOperation(String key, long duration) {
    handler.sendMessage(
        handler //
            .obtainMessage(StatsHandler.TRACK_INTEGRATION_OPERATION, new Pair<>(key, duration)));
  }

  void performIntegrationOperation(Pair<String, Long> durationForIntegration) {
    integrationOperationCount++;
    integrationOperationDuration += durationForIntegration.second;
    Long duration = integrationOperationDurationByIntegration.get(durationForIntegration.first);
    if (duration == null) {
      integrationOperationDurationByIntegration.put(
          durationForIntegration.first, durationForIntegration.second);
    } else {
      integrationOperationDurationByIntegration.put(
          durationForIntegration.first, duration + durationForIntegration.second);
    }
  }

  StatsSnapshot createSnapshot() {
    return new StatsSnapshot(
        System.currentTimeMillis(),
        flushCount,
        flushEventCount,
        integrationOperationCount,
        integrationOperationDuration,
        Collections.unmodifiableMap(integrationOperationDurationByIntegration));
  }

  private static class StatsHandler extends Handler {

    @Private static final int TRACK_FLUSH = 1;
    @Private static final int TRACK_INTEGRATION_OPERATION = 2;

    private final Stats stats;

    StatsHandler(Looper looper, Stats stats) {
      super(looper);
      this.stats = stats;
    }

    @Override
    public void handleMessage(final Message msg) {
      switch (msg.what) {
        case TRACK_FLUSH:
          stats.performFlush(msg.arg1);
          break;
        case TRACK_INTEGRATION_OPERATION:
          //noinspection unchecked
          stats.performIntegrationOperation((Pair<String, Long>) msg.obj);
          break;
        default:
          throw new AssertionError("Unknown Stats handler message: " + msg);
      }
    }
  }
}
