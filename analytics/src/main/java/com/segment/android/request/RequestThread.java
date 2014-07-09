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

package com.segment.android.request;

import android.os.Handler;
import com.segment.android.Analytics;
import com.segment.android.Logger;
import com.segment.android.models.Batch;
import com.segment.android.utils.LooperThreadWithHandler;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

/**
 * A Looper/Handler backed request thread
 */
public class RequestThread extends LooperThreadWithHandler implements IRequestLayer {

  private IRequester requester;

  public RequestThread(IRequester requester) {
    this.requester = requester;
  }

  /**
   * Performs the request to the server.
   *
   * @param batch the action batch to send
   */
  public void send(final Batch batch, final RequestCallback callback) {

    Handler handler = handler();

    handler.post(new Runnable() {

      @Override
      public void run() {
        long start = System.currentTimeMillis();

        // send the actual request
        HttpResponse response = requester.send(batch);

        long duration = System.currentTimeMillis() - start;
        Analytics.getStatistics().updateRequestTime(duration);

        boolean success = false;

        if (response == null) {
          // there's been an error
          Logger.w("Failed to make request to the server.");
        } else if (response.getStatusLine().getStatusCode() != 200) {
          try {
            // there's been a server error
            Logger.e("Received a failed response from the server. %s",
                EntityUtils.toString(response.getEntity()));
          } catch (ParseException e) {
            Logger.w(e, "Failed to parse the response from the server.");
          } catch (IOException e) {
            Logger.w(e, "Failed to read the response from the server.");
          }
        } else {

          Logger.d("Successfully sent a batch to the server");

          success = true;
        }

        if (callback != null) callback.onRequestCompleted(success);
      }
    });
  }

  /**
   * Allow custom {link {@link com.segment.android.request.IRequester} for testing.
   */
  public void setRequester(IRequester requester) {
    this.requester = requester;
  }
}