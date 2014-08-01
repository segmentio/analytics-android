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

import android.os.Build;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class SegmentHTTPApi {
  static final String IMPORT_ENDPOINT = "http://api.segment.io/v1/identify";
  static final URL IMPORT_URL = createUrl(IMPORT_ENDPOINT);

  private final String apiKey;

  SegmentHTTPApi(String apiKey) {
    this.apiKey = apiKey;

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
      // bug in pre-froyo, http://android-developers.blogspot.com/2011/09/androids-http-clients.html
      System.setProperty("http.keepAlive", "false");
    }
  }

  static SegmentHTTPApi create(String apiKey) {
    return new SegmentHTTPApi(apiKey);
  }

  private static URL createUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid url: " + url);
    }
  }

  void upload(Payload payload) throws IOException {
    HttpURLConnection httpURLConnection = (HttpURLConnection) IMPORT_URL.openConnection();

    httpURLConnection.setDoOutput(true);
    httpURLConnection.setRequestMethod("POST");
    httpURLConnection.setRequestProperty("Content-Type", "application/json");
    httpURLConnection.setRequestProperty("Authorization", "Basic " + apiKey + ":");
    httpURLConnection.setDoOutput(true);
    httpURLConnection.setChunkedStreamingMode(0);

    OutputStream out = new BufferedOutputStream(httpURLConnection.getOutputStream());
    out.write(payload.toString().getBytes()); //
    out.close();

    InputStream in = new BufferedInputStream(httpURLConnection.getInputStream());
    in.close();
    httpURLConnection.disconnect();
  }
}
