package com.segment.analytics.wear;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.segment.analytics.Analytics;
import com.segment.analytics.internal.Logger;
import com.segment.analytics.internal.Utils;
import java.util.Collection;
import java.util.HashSet;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

class Dispatcher {
  static final int REQUEST_DISPATCH = 0;
  private static final String DISPATCHER_THREAD_NAME = Utils.THREAD_PREFIX + "Wear-Dispatcher";

  final Handler handler;
  final HandlerThread dispatcherThread;
  final GoogleApiClient googleApiClient;

  Dispatcher(Context context) {
    googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
    dispatcherThread = new HandlerThread(DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    dispatcherThread.start();
    handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
  }

  void dispatchPayload(WearPayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_DISPATCH, payload));
  }

  void performDispatch(WearPayload payload) {
    googleApiClient.blockingConnect();

    for (String node : getNodes(googleApiClient)) {
      MessageApi.SendMessageResult result =
          Wearable.MessageApi.sendMessage(googleApiClient, node, WearAnalytics.ANALYTICS_PATH,
              payload.toString().getBytes()).await();
      if (!result.getStatus().isSuccess()) {
        Logger.e("Failed to send message: " + result.getStatus());
      }
    }
  }

  private Collection<String> getNodes(GoogleApiClient googleApiClient) {
    HashSet<String> results = new HashSet<String>();
    NodeApi.GetConnectedNodesResult nodes =
        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
    for (Node node : nodes.getNodes()) {
      results.add(node.getId());
    }
    return results;
  }

  private static class DispatcherHandler extends Handler {
    private final Dispatcher dispatcher;

    public DispatcherHandler(Looper looper, Dispatcher dispatcher) {
      super(looper);
      this.dispatcher = dispatcher;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_DISPATCH:
          WearPayload payload = (WearPayload) msg.obj;
          dispatcher.performDispatch(payload);
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