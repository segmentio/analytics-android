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

package com.segment.android;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.android.internal.SegmentHTTPApi;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.queue.PayloadUploadTask;
import com.segment.android.internal.queue.PayloadUploadTaskConverter;
import com.segment.android.internal.util.Logger;
import com.segment.android.internal.util.Utils;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;
import java.io.File;
import java.io.IOException;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.android.internal.util.Utils.hasPermission;
import static com.segment.android.internal.util.Utils.isAirplaneModeOn;

class Dispatcher implements PayloadUploadTask.Callback {
  private static final int DEFAULT_QUEUE_SIZE = 20;
  private static final String DISPATCHER_THREAD_NAME = "Dispatcher";

  private static final int AIRPLANE_MODE_ON = 1;
  private static final int AIRPLANE_MODE_OFF = 0;

  static final int REQUEST_ENQUEUE = 1;
  static final int NETWORK_STATE_CHANGE = 9;
  static final int AIRPLANE_MODE_CHANGE = 10;

  final DispatcherThread dispatcherThread;
  final Handler mainThreadHandler;
  final DispatcherHandler handler;
  final ObjectQueue<PayloadUploadTask> queue;
  final Context context;
  final NetworkBroadcastReceiver receiver;
  final boolean scansNetworkChanges;
  final SegmentHTTPApi segmentHTTPApi;
  final int maxQueueSize;
  boolean isShutdown;
  boolean airplaneMode;

  private static final String TASK_QUEUE_FILE_NAME = "payload_task_queue";

  static Dispatcher create(Context context, Handler mainThreadHandler, int queueSize,
      SegmentHTTPApi segmentHTTPApi) {
    FileObjectQueue.Converter converter = new PayloadUploadTaskConverter();
    File queueFile = new File(context.getFilesDir(), TASK_QUEUE_FILE_NAME);
    FileObjectQueue<PayloadUploadTask> delegate;
    try {
      delegate = new FileObjectQueue<PayloadUploadTask>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    return new Dispatcher(context, mainThreadHandler, queueSize, segmentHTTPApi, delegate);
  }

  Dispatcher(Context context, Handler mainThreadHandler, int maxQueueSize,
      SegmentHTTPApi segmentHTTPApi, ObjectQueue<PayloadUploadTask> queue) {
    this.context = context;
    this.dispatcherThread = new DispatcherThread();
    this.dispatcherThread.start();
    this.handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
    this.mainThreadHandler = mainThreadHandler;
    this.maxQueueSize = maxQueueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.airplaneMode = isAirplaneModeOn(context);
    this.scansNetworkChanges = hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
    this.queue = queue;
    isShutdown = false;
    receiver = new NetworkBroadcastReceiver(this);
    receiver.register();
  }

  void pause() {
    isShutdown = true;
    dispatcherThread.quit();
    receiver.unregister();
  }

  // Dispatch, these methods simply pass the action to a thread handler

  void dispatchEnqueue(BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_ENQUEUE, payload));
  }

  void dispatchAirplaneModeChange(boolean airplaneMode) {
    handler.sendMessage(handler.obtainMessage(AIRPLANE_MODE_CHANGE,
        airplaneMode ? AIRPLANE_MODE_ON : AIRPLANE_MODE_OFF, 0));
  }

  void dispatchNetworkStateChange(NetworkInfo info) {
    handler.sendMessage(handler.obtainMessage(NETWORK_STATE_CHANGE, info));
  }

  // Perform dispatched actions, these run on a non-ui thread

  void performEnqueue(BasePayload payload) {
    queue.add(new PayloadUploadTask(payload));

    if (queue.size() >= maxQueueSize) {
      performFlush();
    }
  }

  void performFlush() {
    executeNext();
  }

  private void executeNext() {
    if (!airplaneMode) {
      PayloadUploadTask task = queue.peek();
      if (task != null) {
        task.setSegmentHTTPApi(segmentHTTPApi);
        task.execute(this);
      }
    }
  }

  @Override public void onSuccess() {
    queue.remove();
    executeNext();
  }

  @Override public void onFailure() {
    Logger.e("Failed to upload payload");
  }

  void performAirplaneModeChange(boolean airplaneMode) {
    this.airplaneMode = airplaneMode;
  }

  void performNetworkStateChange(NetworkInfo info) {
    // Intentionally check only if isConnected() here before we flush out failed actions.
    if (info != null && info.isConnected()) {
      // todo: flush();
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
        case NETWORK_STATE_CHANGE: {
          NetworkInfo info = (NetworkInfo) msg.obj;
          dispatcher.performNetworkStateChange(info);
          break;
        }
        case AIRPLANE_MODE_CHANGE: {
          dispatcher.performAirplaneModeChange(msg.arg1 == AIRPLANE_MODE_ON);
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

  static class NetworkBroadcastReceiver extends BroadcastReceiver {
    static final String EXTRA_AIRPLANE_STATE = "state";

    private final Dispatcher dispatcher;

    NetworkBroadcastReceiver(Dispatcher dispatcher) {
      this.dispatcher = dispatcher;
    }

    void register() {
      IntentFilter filter = new IntentFilter();
      filter.addAction(ACTION_AIRPLANE_MODE_CHANGED);
      if (dispatcher.scansNetworkChanges) {
        filter.addAction(CONNECTIVITY_ACTION);
      }
      dispatcher.context.registerReceiver(this, filter);
    }

    void unregister() {
      dispatcher.context.unregisterReceiver(this);
    }

    @Override public void onReceive(Context context, Intent intent) {
      // On some versions of Android this may be called with a null Intent,
      // also without extras (getExtras() == null), in such case we use defaults.
      if (intent == null) {
        return;
      }
      final String action = intent.getAction();
      if (ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
        if (!intent.hasExtra(EXTRA_AIRPLANE_STATE)) {
          return; // No airplane state, ignore it. Should we query Utils.isAirplaneModeOn?
        }
        dispatcher.dispatchAirplaneModeChange(intent.getBooleanExtra(EXTRA_AIRPLANE_STATE, false));
      } else if (CONNECTIVITY_ACTION.equals(action)) {
        ConnectivityManager connectivityManager =
            Utils.getSystemService(context, CONNECTIVITY_SERVICE);
        dispatcher.dispatchNetworkStateChange(connectivityManager.getActiveNetworkInfo());
      }
    }
  }
}
