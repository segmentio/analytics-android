/*
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

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Utils.OWNER_DISPATCHER;
import static com.segment.analytics.Utils.VERB_ENQUEUE;
import static com.segment.analytics.Utils.VERB_FLUSH;
import static com.segment.analytics.Utils.debug;
import static com.segment.analytics.Utils.error;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;
import static com.segment.analytics.Utils.toISO8601Date;

class Dispatcher {
  static final int REQUEST_ENQUEUE = 0;
  static final int REQUEST_FLUSH = 1;

  private static final String DISPATCHER_THREAD_NAME = Utils.THREAD_PREFIX + "Dispatcher";
  private static final String TASK_QUEUE_FILE_NAME = "payload-task-queue-";

  final Context context;
  final ObjectQueue<BasePayload> queue;
  final SegmentHTTPApi segmentHTTPApi;
  final int queueSize;
  final int flushInterval;
  final Stats stats;
  final Handler handler;
  final HandlerThread dispatcherThread;
  final boolean loggingEnabled;
  final Map<String, Boolean> integrations;

  static Dispatcher create(Context context, int queueSize, int flushInterval,
      SegmentHTTPApi segmentHTTPApi, Map<String, Boolean> integrations, String tag, Stats stats,
      boolean loggingEnabled) {
    FileObjectQueue.Converter<BasePayload> converter = new PayloadConverter();
    try {
      File parent = context.getFilesDir();
      if (!parent.exists()) parent.mkdirs();
      File queueFile = new File(parent, TASK_QUEUE_FILE_NAME + tag);
      ObjectQueue<BasePayload> queue = new FileObjectQueue<BasePayload>(queueFile, converter);
      return new Dispatcher(context, queueSize, flushInterval, segmentHTTPApi, queue,
          integrations, stats, loggingEnabled);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
  }

  Dispatcher(Context context, int queueSize, int flushInterval, SegmentHTTPApi segmentHTTPApi,
      ObjectQueue<BasePayload> queue, Map<String, Boolean> integrations, Stats stats,
      boolean loggingEnabled) {
    this.context = context;
    this.queueSize = queueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.queue = queue;
    this.stats = stats;
    this.loggingEnabled = loggingEnabled;
    this.integrations = integrations;
    this.flushInterval = flushInterval * 1000;
    dispatcherThread = new HandlerThread(DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    dispatcherThread.start();
    handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
    rescheduleFlush();
  }

  void dispatchEnqueue(final BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_ENQUEUE, payload));
  }

  void dispatchFlush() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
  }

  void performEnqueue(BasePayload payload) {
    try {
      queue.add(payload);
    } catch (IOException e) {
      if (loggingEnabled) {
        error(OWNER_DISPATCHER, VERB_ENQUEUE, payload.id(), e,
            String.format("payload: %s", payload));
      }
    }

    if (loggingEnabled) {
      debug(OWNER_DISPATCHER, VERB_ENQUEUE, payload.id(),
          String.format("queueSize: %s", queue.size()));
    }
    // Check if we've reached the maximum queue size
    if (queue.size() >= queueSize) {
      performFlush();
    }
  }

  void performFlush() {
    if (queue.size() <= 0 || !isConnected(context)) return;

    final List<BasePayload> payloads = new ArrayList<BasePayload>();
    try {
      queue.setListener(new ObjectQueue.Listener<BasePayload>() {
        @Override public void onAdd(ObjectQueue<BasePayload> queue, BasePayload entry) {
          if (loggingEnabled) {
            debug(OWNER_DISPATCHER, VERB_FLUSH, entry.id(), null);
          }
          payloads.add(entry);
        }

        @Override public void onRemove(ObjectQueue<BasePayload> queue) {

        }
      });
      queue.setListener(null);
    } catch (IOException e) {
      if (loggingEnabled) {
        error(OWNER_DISPATCHER, VERB_FLUSH, "could not read queue", e,
            String.format("queue: %s", queue));
      }
      return;
    }

    int count = payloads.size();
    try {
      segmentHTTPApi.upload(new BatchPayload(payloads, integrations));
      stats.dispatchFlush(count);
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < count; i++) {
        queue.remove();
      }
    } catch (IOException e) {
      if (loggingEnabled) {
        error(OWNER_DISPATCHER, VERB_FLUSH, "unable to clear queue", e, "events: " + count);
      }
    }
    rescheduleFlush();
  }

  private void rescheduleFlush() {
    handler.removeMessages(REQUEST_FLUSH);
    handler.sendMessageDelayed(handler.obtainMessage(REQUEST_FLUSH), flushInterval);
  }

  static class BatchPayload extends JsonMap {
    /**
     * The sent timestamp is an ISO-8601-formatted string that, if present on a message, can be
     * used to correct the original timestamp in situations where the local clock cannot be
     * trusted, for example in our mobile libraries. The sentAt and receivedAt timestamps will be
     * assumed to have occurred at the same time, and therefore the difference is the local clock
     * skew.
     */
    private static final String SENT_AT_KEY = "sentAt";

    /**
     * A dictionary of integration names that the message should be proxied to. 'All' is a special
     * name that applies when no key for a specific integration is found, and is case-insensitive.
     */
    private static final String INTEGRATIONS_KEY = "integrations";

    BatchPayload(List<BasePayload> batch, Map<String, Boolean> integrations) {
      put("batch", batch);
      put(INTEGRATIONS_KEY, integrations);
      put(SENT_AT_KEY, toISO8601Date(new Date()));
    }
  }

  void shutdown() {
    quitThread(dispatcherThread);
  }

  private static class DispatcherHandler extends Handler {
    private final Dispatcher dispatcher;

    DispatcherHandler(Looper looper, Dispatcher dispatcher) {
      super(looper);
      this.dispatcher = dispatcher;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_ENQUEUE:
          BasePayload payload = (BasePayload) msg.obj;
          dispatcher.performEnqueue(payload);
          break;
        case REQUEST_FLUSH:
          dispatcher.performFlush();
          break;
        default:
          panic("Unknown dispatcher message." + msg.what);
      }
    }
  }
}
