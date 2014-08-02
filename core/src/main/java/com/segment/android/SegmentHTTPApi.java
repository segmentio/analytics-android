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
import android.util.Base64;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

class SegmentHTTPApi {
  static final String IMPORT_ENDPOINT = "https://api.segment.io/v1/track";
  static final URL IMPORT_URL = createUrl(IMPORT_ENDPOINT);

  private final String writeKey;

  SegmentHTTPApi(String writeKey) {
    this.writeKey = writeKey;

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
      // bug in pre-froyo, http://android-developers.blogspot.com/2011/09/androids-http-clients.html
      System.setProperty("http.keepAlive", "false");
    }
  }

  static SegmentHTTPApi create(String writeKey) {
    return new SegmentHTTPApi(writeKey);
  }

  private static URL createUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid url: " + url);
    }
  }

  void upload(Payload payload) throws IOException {
    String url = "https://api.segment.io/v1/track";
    String json = payload.toString();

    Logger.d("Uploading Payload: %s", json);

    HttpClient httpclient = new DefaultHttpClient();
    HttpPost post = new HttpPost(url);
    post.setHeader("Content-Type", "application/json");

    // Basic Authentication
    // https://segment.io/docs/tracking-api/reference/#authentication
    post.addHeader("Authorization",
        "Basic " + Base64.encodeToString((writeKey + ":").getBytes(), Base64.NO_WRAP));

    ByteArrayEntity se = new ByteArrayEntity(json.getBytes());
    se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
    post.setEntity(se);
    HttpResponse response = httpclient.execute(post);
    Logger.d("Response line: %s", response.getStatusLine());
    Logger.d("Response: %s", EntityUtils.toString(response.getEntity()));


    /**
     HttpsURLConnection urlConnection = (HttpsURLConnection) IMPORT_URL.openConnection();

     urlConnection.setDoOutput(true);
     urlConnection.setRequestMethod("POST");
     urlConnection.setRequestProperty("Content-Type", "application/json");
     urlConnection.setRequestProperty("Authorization", "Basic " + writeKey + ":");
     urlConnection.setDoOutput(true);
     urlConnection.setChunkedStreamingMode(0);

     Logger.d("Uploading Payload: %s", payload.toString());

     OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
     out.write(payload.toString().getBytes());
     out.close();

     Logger.d("Response code: %s", urlConnection.getResponseCode());
     Logger.d("Response message: %s", urlConnection.getResponseMessage());

     InputStream in = new BufferedInputStream(urlConnection.getInputStream());
     in.close();
     urlConnection.disconnect();
     */
  }
}
