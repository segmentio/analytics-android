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

package com.segment.android.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.segment.android.Logger;

public class LooperThreadWithHandler extends Thread implements IThreadedLayer {

  private Handler handler;

  private void waitForReady() {
    while (this.handler == null) {
      synchronized (this) {
        try {
          wait();
        } catch (InterruptedException e) {
          Logger.e(
              "Failed while waiting for singleton thread ready. " + Log.getStackTraceString(e));
        }
      }
    }
  }

  @Override
  public void run() {
    Looper.prepare();
    synchronized (this) {
      handler = new Handler();
      notifyAll();
    }
    Looper.loop();
  }

  /**
   * Gets this thread's handler
   */
  public Handler handler() {
    waitForReady();
    return handler;
  }

  /**
   * Quits the current looping thread
   */
  public void quit() {
    Looper.myLooper().quit();
  }
}
