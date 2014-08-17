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

package com.segment.android.internal;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.google.gson.Gson;
import com.segment.android.Segment;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.queue.GsonConverter;
import com.segment.android.internal.util.ISO8601Time;
import com.segment.android.internal.util.Logger;
import com.segment.android.internal.util.Utils;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.android.internal.util.Utils.isNullOrEmpty;

public class Dispatcher {
  private static final String DISPATCHER_THREAD_NAME = "Dispatcher";

  static final int REQUEST_ENQUEUE = 1;
  static final int REQUEST_FLUSH = 2;

  final DispatcherThread dispatcherThread;
  final Handler mainThreadHandler;
  final DispatcherHandler handler;
  final ObjectQueue<BasePayload> queue;
  final SegmentHTTPApi segmentHTTPApi;
  final int maxQueueSize;
  final ExecutorService flushService;

  private static final String TASK_QUEUE_FILE_NAME = "payload_task_queue";

  public static Dispatcher create(Context context, Handler mainThreadHandler, int queueSize,
      Gson gson, SegmentHTTPApi segmentHTTPApi) {
    FileObjectQueue.Converter<BasePayload> converter = new GsonConverter<BasePayload>(gson);
    File queueFile = new File(context.getFilesDir(), TASK_QUEUE_FILE_NAME);
    FileObjectQueue<BasePayload> queue;
    try {
      queue = new FileObjectQueue<BasePayload>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    ExecutorService flushService =
        Executors.newSingleThreadExecutor(Executors.defaultThreadFactory());

    return new Dispatcher(mainThreadHandler, flushService, queueSize, segmentHTTPApi, queue);
  }

  Dispatcher(Handler mainThreadHandler, ExecutorService flushService, int maxQueueSize,
      SegmentHTTPApi segmentHTTPApi, ObjectQueue<BasePayload> queue) {
    this.dispatcherThread = new DispatcherThread();
    this.dispatcherThread.start();
    this.handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
    this.mainThreadHandler = mainThreadHandler;
    this.maxQueueSize = maxQueueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.queue = queue;
    this.flushService = flushService;
  }

  public void dispatchEnqueue(BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_ENQUEUE, payload));
  }

  public void dispatchFlush() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
  }

  void performEnqueue(BasePayload payload) {
    queue.add(payload);

    if (queue.size() >= maxQueueSize) {
      Logger.d("queue size %s > %s; flushing", queue.size(), maxQueueSize);
      performFlush();
    }
  }

  void performFlush() {
    flushService.submit(new Runnable() {
      @Override public void run() {
        flush();
      }
    });
  }

  private void flush() {
    List<BasePayload> payloads = new ArrayList<BasePayload>();

    while (queue.size() > 0) {
      BasePayload payload = queue.peek();
      payload.setSentAt(ISO8601Time.now().time());
      payloads.add(payload);
      queue.remove();
    }

    Logger.d("Payload size : %s", payloads.size());
    if (isNullOrEmpty(payloads)) {
      return;
    }
    try {
      segmentHTTPApi.upload(payloads);
    } catch (IOException e) {
      Logger.e(e, "Failed to upload payloads");
      for (BasePayload payload : payloads) {
        queue.add(payload);
      }
    }
  }

  private static class DispatcherHandler extends Handler {
    private final Dispatcher dispatcher;

    DispatcherHandler(Looper looper, Dispatcher dispatcher) {
      super(looper);
      this.dispatcher = dispatcher;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_ENQUEUE: {
          BasePayload payload = (BasePayload) msg.obj;
          dispatcher.performEnqueue(payload);
          break;
        }
        case REQUEST_FLUSH: {
          dispatcher.performFlush();
          break;
        }
        default:
          Segment.HANDLER.post(new Runnable() {
            @Override public void run() {
              throw new AssertionError("Unknown handler message received: " + msg.what);
            }
          });
      }
    }
  }

  static class DispatcherThread extends HandlerThread {
    DispatcherThread() {
      super(Utils.THREAD_PREFIX + DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    }
  }
}
