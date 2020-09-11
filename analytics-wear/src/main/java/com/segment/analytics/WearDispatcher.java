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
import com.segment.analytics.internal.Utils;
import java.util.Collection;
import java.util.HashSet;

class WearDispatcher {

    private static final String DISPATCHER_THREAD_NAME = Utils.THREAD_PREFIX + "Wear-Dispatcher";

    final Handler handler;
    final HandlerThread dispatcherThread;
    final GoogleApiClient googleApiClient;
    final Cartographer cartographer;

    WearDispatcher(Context context) {
        googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
        cartographer = Cartographer.INSTANCE;
        dispatcherThread = new HandlerThread(DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
        dispatcherThread.start();
        handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
    }

    void dispatchPayload(WearPayload payload) {
        handler.sendMessage(handler.obtainMessage(DispatcherHandler.REQUEST_DISPATCH, payload));
    }

    void performDispatch(WearPayload payload) {
        googleApiClient.blockingConnect();

        for (String node : getNodes(googleApiClient)) {
            MessageApi.SendMessageResult result =
                    Wearable.MessageApi.sendMessage(
                                    googleApiClient,
                                    node,
                                    WearAnalytics.ANALYTICS_PATH,
                                    cartographer.toJson(payload).getBytes())
                            .await();
            if (!result.getStatus().isSuccess()) {
                // todo: log error
            }
        }
    }

    private Collection<String> getNodes(GoogleApiClient googleApiClient) {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    private static class DispatcherHandler extends Handler {

        static final int REQUEST_DISPATCH = 0;
        private final WearDispatcher wearDispatcher;

        public DispatcherHandler(Looper looper, WearDispatcher wearDispatcher) {
            super(looper);
            this.wearDispatcher = wearDispatcher;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REQUEST_DISPATCH:
                    WearPayload payload = (WearPayload) msg.obj;
                    wearDispatcher.performDispatch(payload);
                    break;
                default:
                    Analytics.HANDLER.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    throw new AssertionError(
                                            "Unhandled dispatcher message." + msg.what);
                                }
                            });
            }
        }
    }
}
