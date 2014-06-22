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

import com.segment.android.models.BasePayload;
import com.segment.android.utils.IThreadedLayer;
import java.util.List;

/**
 * Handles communication with the database
 * on its own thread to achieve SQL thread safety.
 */
public interface IPayloadDatabaseLayer extends IThreadedLayer {
  //
  // Callbacks
  //

  /**
   * Called when an enqueue completes.
   */
  public interface EnqueueCallback {
    /**
     * Called when an enqueue finishes.
     *
     * @param success Whether the enqueue was successful.
     * @param rowCount The new database size
     */
    void onEnqueue(boolean success, long rowCount);
  }

  /**
   * Callback for when a database payload query returns
   *
   * @author ivolo
   */
  public interface PayloadCallback {
    void onPayload(long minId, long maxId, List<BasePayload> payloads);
  }

  /**
   * Callback for when payloads are removed from the database
   *
   * @author ivolo
   */
  public interface RemoveCallback {
    void onRemoved(int removed);
  }

  //
  // Methods
  //

  /**
   * Adds a payload to the database
   */
  void enqueue(BasePayload payload, EnqueueCallback callback);

  /**
   * Gets the next payloads from the database
   */
  void nextPayload(PayloadCallback callback);

  /**
   * Removes payloads from the database
   */
  void removePayloads(final long minId, long maxId, RemoveCallback callback);
}