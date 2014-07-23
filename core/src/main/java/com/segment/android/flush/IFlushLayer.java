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

package com.segment.android.flush;

import com.segment.android.models.Batch;
import com.segment.android.utils.IThreadedLayer;

/**
 * Handles flushing to the server endpoint
 */
public interface IFlushLayer extends IThreadedLayer {

  //
  // Callbacks
  //

  /**
   * Callback for the
   * {@link IFlushLayer#flush(com.segment.android.flush.IFlushLayer.FlushCallback)} method
   */
  public interface FlushCallback {
    /**
     * Called when all messages have been flushed from the queue
     *
     * @param success True for successful flush, false for not.
     * @param batch The batch that was sent to the server
     */
    void onFlushCompleted(boolean success, Batch batch);
  }

  //
  // Methods
  //

  /**
   * Triggers a flush from the local action database to the server.
   */
  void flush(FlushCallback callback);
}
