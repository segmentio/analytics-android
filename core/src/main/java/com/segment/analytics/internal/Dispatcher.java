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

package com.segment.analytics.internal;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.analytics.Analytics;
import com.segment.analytics.internal.payload.BasePayload;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.internal.Utils.isConnected;

public class Dispatcher {
  static final int REQUEST_ENQUEUE = 0;
  static final int REQUEST_FLUSH = 1;

  private static final String DISPATCHER_THREAD_NAME = Utils.THREAD_PREFIX + "Dispatcher";
  private static final String TASK_QUEUE_FILE_NAME = "payload-task-queue-";

  final Context context;
  final ObjectQueue<BasePayload> queue;
  final SegmentHTTPApi segmentHTTPApi;
  final int maxQueueSize;
  final Stats stats;
  final Handler handler;
  final HandlerThread dispatcherThread;

  public static Dispatcher create(Context context, int maxQueueSize, SegmentHTTPApi segmentHTTPApi,
      Stats stats, String tag) {
    FileObjectQueue.Converter<BasePayload> converter = new PayloadConverter();
    File queueFile = new File(context.getFilesDir(), TASK_QUEUE_FILE_NAME + tag);
    FileObjectQueue<BasePayload> queue;
    try {
      queue = new FileObjectQueue<BasePayload>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    return new Dispatcher(context, maxQueueSize, segmentHTTPApi, queue, stats);
  }

  Dispatcher(Context context, int maxQueueSize, SegmentHTTPApi segmentHTTPApi,
      ObjectQueue<BasePayload> queue, Stats stats) {
    this.context = context;
    this.maxQueueSize = maxQueueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.queue = queue;
    this.stats = stats;
    dispatcherThread = new HandlerThread(DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    dispatcherThread.start();
    handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
  }

  public void dispatchEnqueue(final BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_ENQUEUE, payload));
  }

  public void dispatchFlush() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
  }

  void performEnqueue(BasePayload payload) {
    queue.add(payload);
    Logger.v("Enqueued %s payload", payload.type());
    Logger.v("Queue size %s", queue.size());
    if (queue.size() >= maxQueueSize) {
      Logger.d("Queue size (%s) > maxQueueSize (%s). Flushing...", queue.size(), maxQueueSize);
      dispatchFlush();
    }
  }

  void performFlush() {
    if (queue.size() <= 0) {
      Logger.d("No events in queue, skipping flush.");
      return; // no-op
    }
    if (!isConnected(context)) {
      Logger.d("Not connected to network, skipping flush.");
      return; // no-op
    }

    final List<BasePayload> payloads = new ArrayList<BasePayload>();
    queue.setListener(new ObjectQueue.Listener<BasePayload>() {
      @Override public void onAdd(ObjectQueue<BasePayload> queue, BasePayload entry) {
        payloads.add(entry);
      }

      @Override public void onRemove(ObjectQueue<BasePayload> queue) {

      }
    });
    queue.setListener(null);

    int count = payloads.size();
    try {
      Logger.v("Flushing %s payloads.", count);
      segmentHTTPApi.upload(payloads);
      Logger.v("Successfully flushed %s payloads.", count);
      stats.dispatchFlush(count);
    } catch (IOException e) {
      Logger.e(e, "Failed to flush payloads.");
    }
  }

  public void shutdown() {
    dispatcherThread.quit();
  }

  private static class DispatcherHandler extends Handler {
    private final Dispatcher dispatcher;

    public DispatcherHandler(Looper looper, Dispatcher dispatcher) {
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
          Analytics.HANDLER.post(new Runnable() {
            @Override public void run() {
              throw new AssertionError("Unhandled dispatcher message." + msg.what);
            }
          });
      }
    }
  }
}
