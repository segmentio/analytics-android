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
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Logger.THREAD_DISPATCHER;
import static com.segment.analytics.Logger.VERB_DISPATCHED;
import static com.segment.analytics.Logger.VERB_FLUSHED;
import static com.segment.analytics.Logger.VERB_FLUSHING;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;

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

  static Dispatcher create(Context context, int maxQueueSize, SegmentHTTPApi segmentHTTPApi,
      Stats stats, String tag, Logger logger) {
    Tape.Converter<BasePayload> converter = new PayloadConverter();
    File queueFile = new File(context.getFilesDir(), TASK_QUEUE_FILE_NAME + tag);
    Tape<BasePayload> queue;
    try {
      queue = new Tape<BasePayload>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    return new Dispatcher(context, maxQueueSize, segmentHTTPApi, queue, stats, logger);
  }

  Dispatcher(Context context, int maxQueueSize, SegmentHTTPApi segmentHTTPApi,
      Queue<BasePayload> queue, Stats stats, Logger logger) {
    this.context = context;
    this.maxQueueSize = maxQueueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.queue = queue;
    this.stats = stats;
    this.logger = logger;
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
      logger.debug(THREAD_DISPATCHER, VERB_DISPATCHED, payload.messageId(),
          String.format("{type: %s, queueSize: %s}", payload.type(), queueSize));
    }
    if (queueSize >= maxQueueSize) {
      performFlush();
    }
  }

  void performFlush() {
    if (queue.size() <= 0 || !isConnected(context)) return;

    final List<BasePayload> payloads = new ArrayList<BasePayload>();
    Iterator<BasePayload> iterator = queue.iterator();
    while (iterator.hasNext()) {
      BasePayload payload = iterator.next();
      if (logger.loggingEnabled) {
        logger.debug(THREAD_DISPATCHER, VERB_FLUSHING, payload.messageId(),
            "{type: " + payload.type() + '}');
      }
      payloads.add(payload);
    }

    int count = payloads.size();
    try {
      segmentHTTPApi.upload(payloads);
      if (logger.loggingEnabled) {
        logger.debug(THREAD_DISPATCHER, VERB_FLUSHED, null, "{events: " + count + '}');
      }
      stats.dispatchFlush(count);
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < count; i++) {
        queue.remove();
      }
    } catch (IOException e) {
      if (logger.loggingEnabled) {
        logger.error(THREAD_DISPATCHER, VERB_FLUSHING, null, e, "{events: " + count + '}');
      }
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
