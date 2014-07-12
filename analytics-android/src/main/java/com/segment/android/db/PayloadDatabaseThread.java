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

package com.segment.android.db;

import android.os.Handler;
import android.util.Pair;
import com.segment.android.Constants;
import com.segment.android.models.BasePayload;
import com.segment.android.utils.LooperThreadWithHandler;
import java.util.LinkedList;
import java.util.List;

/**
 * The DatabaseThread is a singleton handler thread that is
 * statically created to assure a single entry point into the
 * database to achieve SQLLite thread safety.
 */
public class PayloadDatabaseThread extends LooperThreadWithHandler
    implements IPayloadDatabaseLayer {

  private PayloadDatabase database;

  public PayloadDatabaseThread(PayloadDatabase database) {
    this.database = database;
  }

  public void enqueue(final BasePayload payload, final EnqueueCallback callback) {

    Handler handler = handler();
    handler.post(new Runnable() {

      @Override
      public void run() {
        boolean success = database.addPayload(payload);
        long rowCount = database.getRowCount();

        if (callback != null) callback.onEnqueue(success, rowCount);
      }
    });
  }

  public void nextPayload(final PayloadCallback callback) {

    Handler handler = handler();
    handler.post(new Runnable() {

      @Override
      public void run() {

        List<Pair<Long, BasePayload>> pairs = database.getEvents(Constants.MAX_FLUSH);

        long minId = 0;
        long maxId = 0;

        List<BasePayload> payloads = new LinkedList<BasePayload>();

        if (pairs.size() > 0) {
          minId = pairs.get(0).first;
          maxId = pairs.get(pairs.size() - 1).first;

          for (Pair<Long, BasePayload> pair : pairs) {
            payloads.add(pair.second);
          }
        }

        if (callback != null) callback.onPayload(minId, maxId, payloads);
      }
    });
  }

  public void removePayloads(final long minId, final long maxId, final RemoveCallback callback) {

    Handler handler = handler();
    handler.post(new Runnable() {

      @Override
      public void run() {

        int removed = database.removeEvents(minId, maxId);

        if (callback != null) callback.onRemoved(removed);
      }
    });
  }
}