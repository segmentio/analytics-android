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

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.queue.PayloadConverter;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.segment.android.internal.Utils.hasPermission;

public class Dispatcher {
  private static final String TASK_QUEUE_FILE_NAME = "payload_task_queue";

  final Context context;
  final Handler mainThreadHandler;
  final ObjectQueue<BasePayload> queue;
  final SegmentHTTPApi segmentHTTPApi;
  final int maxQueueSize;
  final ExecutorService flushService;
  final ExecutorService queueService;

  public static Dispatcher create(Context context, Handler mainThreadHandler, int maxQueueSize,
      SegmentHTTPApi segmentHTTPApi) {
    FileObjectQueue.Converter<BasePayload> converter = new PayloadConverter();
    File queueFile = new File(context.getFilesDir(), TASK_QUEUE_FILE_NAME);
    FileObjectQueue<BasePayload> queue;
    try {
      queue = new FileObjectQueue<BasePayload>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    ExecutorService flushService = Executors.newSingleThreadExecutor();
    ExecutorService queueService = Executors.newSingleThreadExecutor();
    return new Dispatcher(context, mainThreadHandler, flushService, maxQueueSize, segmentHTTPApi,
        queue, queueService);
  }

  Dispatcher(Context context, Handler mainThreadHandler, ExecutorService flushService,
      int maxQueueSize, SegmentHTTPApi segmentHTTPApi, ObjectQueue<BasePayload> queue,
      ExecutorService queueService) {
    this.context = context;
    this.mainThreadHandler = mainThreadHandler;
    this.maxQueueSize = maxQueueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.queue = queue;
    this.flushService = flushService;
    this.queueService = queueService;
  }

  public void dispatchEnqueue(final BasePayload payload) {
    queueService.submit(new Runnable() {
      @Override public void run() {
        enqueue(payload);
      }
    });
  }

  public void dispatchFlush() {
    flushService.submit(new Runnable() {
      @Override public void run() {
        flush();
      }
    });
  }

  private void enqueue(BasePayload payload) {
    queue.add(payload);

    if (queue.size() >= maxQueueSize) {
      Logger.d("queue size %s > %s; flushing", queue.size(), maxQueueSize);
      dispatchFlush();
    }
  }

  private void flush() {
    if (queue.size() <= 0 || !isConnected()) {
      return; // no-op
    }

    List<BasePayload> payloads = new ArrayList<BasePayload>();
    // This causes us to lose the guarantee that events will be delivered, since we could lose
    // power while adding them to the list and the events would be lost from the queue.
    // Another operation
    while (queue.size() > 0) {
      BasePayload payload = queue.peek();
      payload.setSentAt(ISO8601Time.now());
      payloads.add(payload);
      queue.remove();
    }

    try {
      segmentHTTPApi.upload(payloads);
    } catch (IOException e) {
      Logger.e(e, "Failed to upload payloads");
      for (BasePayload payload : payloads) {
        queue.add(payload); // re-enqueue the payloads
      }
    }
  }

  /**
   * Returns true if the phone is connected to a network, or if we don't have the permission to
   * find out. False otherwise.
   */
  boolean isConnected() {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      return true; // assume we have the connection and try to upload
    }
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }
}
