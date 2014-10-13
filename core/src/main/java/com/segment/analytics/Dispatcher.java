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
import java.util.Queue;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Logger.OWNER_DISPATCHER;
import static com.segment.analytics.Logger.VERB_DISPATCHED;
import static com.segment.analytics.Logger.VERB_FLUSHED;
import static com.segment.analytics.Logger.VERB_FLUSHING;
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
  final Queue<BasePayload> queue;
  final SegmentHTTPApi segmentHTTPApi;
  final int maxQueueSize;
  final Stats stats;
  final Handler handler;
  final HandlerThread dispatcherThread;
  final Logger logger;
  final Map<String, Boolean> integrations;

  static Dispatcher create(Context context, int maxQueueSize, SegmentHTTPApi segmentHTTPApi,
      Map<String, Boolean> integrations, String tag, Stats stats, Logger logger) {
    Tape.Converter<BasePayload> converter = new PayloadConverter();
    File queueFile = new File(context.getFilesDir(), TASK_QUEUE_FILE_NAME + tag);
    Tape<BasePayload> queue;
    try {
      queue = new Tape<BasePayload>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    return new Dispatcher(context, maxQueueSize, segmentHTTPApi, queue, integrations, stats,
        logger);
  }

  Dispatcher(Context context, int maxQueueSize, SegmentHTTPApi segmentHTTPApi,
      Queue<BasePayload> queue, Map<String, Boolean> integrations, Stats stats, Logger logger) {
    this.context = context;
    this.maxQueueSize = maxQueueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.queue = queue;
    this.stats = stats;
    this.logger = logger;
    this.integrations = integrations;
    dispatcherThread = new HandlerThread(DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    dispatcherThread.start();
    handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
  }

  void dispatchEnqueue(final BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_ENQUEUE, payload));
  }

  void dispatchFlush() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
  }

  void performEnqueue(BasePayload payload) {
    queue.add(payload);
    int queueSize = queue.size();
    if (logger.loggingEnabled) {
      logger.debug(OWNER_DISPATCHER, VERB_DISPATCHED, payload.messageId(),
          String.format("type: %s, queueSize: %s", payload.type(), queueSize));
    }
    if (queueSize >= maxQueueSize) {
      performFlush();
    }
  }

  void performFlush() {
    if (queue.size() <= 0 || !isConnected(context)) return;

    final List<BasePayload> payloads = new ArrayList<BasePayload>();
    for (BasePayload payload : queue) {
      if (logger.loggingEnabled) {
        logger.debug(OWNER_DISPATCHER, VERB_FLUSHING, payload.messageId(),
            "type: " + payload.type());
      }
      payloads.add(payload);
    }

    int count = payloads.size();
    try {
      segmentHTTPApi.upload(new BatchPayload(payloads, integrations));
      if (logger.loggingEnabled) {
        logger.debug(OWNER_DISPATCHER, VERB_FLUSHED, null, "events: " + count);
      }
      stats.dispatchFlush(count);
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < count; i++) {
        queue.remove();
      }
    } catch (IOException e) {
      if (logger.loggingEnabled) {
        logger.error(OWNER_DISPATCHER, VERB_FLUSHING, null, e, "events: " + count);
      }
    }
  }

  static class BatchPayload extends JsonMap {
    /**
     * The sent timestamp is an ISO-8601-formatted string that, if present on a message, can be
     * used
     * to correct the original timestamp in situations where the local clock cannot be trusted, for
     * example in our mobile libraries. The sentAt and receivedAt timestamps will be assumed to
     * have
     * occurred at the same time, and therefore the difference is the local clock skew.
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
