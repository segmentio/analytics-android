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

package com.segment.analytics;

import android.os.Build;
import android.util.Base64;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import static com.segment.analytics.SegmentIntegration.BatchPayload;
import static java.net.HttpURLConnection.HTTP_OK;

class SegmentHTTPApi {
  static final String API_URL = "https://api.segment.io/";

  private final String writeKey;

  SegmentHTTPApi(String writeKey) {
    this.writeKey = writeKey;

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
      // bug in pre-froyo, http://android-developers.blogspot.com/2011/09/androids-http-clients.html
      System.setProperty("http.keepAlive", "false");
    }
  }

  private static URL createUrl(String endpoint) {
    String url = API_URL + endpoint;
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Could not form url for " + url);
    }
  }

  private static String readFully(InputStream in) throws IOException {
    BufferedReader r = new BufferedReader(new InputStreamReader(in));
    StringBuilder response = new StringBuilder();
    for (String line; (line = r.readLine()) != null; ) {
      response.append(line);
    }
    return response.toString();
  }

  void upload(BatchPayload batchPayload) throws IOException {
    HttpsURLConnection urlConnection = (HttpsURLConnection) createUrl("v1/import").openConnection();

    urlConnection.setDoOutput(true);
    urlConnection.setDoInput(true);
    urlConnection.setRequestMethod("POST");
    urlConnection.setRequestProperty("Content-Type", "application/json");
    urlConnection.setRequestProperty("Authorization",
        "Basic " + Base64.encodeToString((writeKey + ":").getBytes(), Base64.NO_WRAP));
    urlConnection.setChunkedStreamingMode(0);

    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
    out.write(batchPayload.toString().getBytes());
    out.close();

    int responseCode = urlConnection.getResponseCode();
    if (responseCode != HTTP_OK) {
      String message = readFully(urlConnection.getErrorStream());
      throw new IOException("Could not upload payloads. Response code: "
          + responseCode
          + ", response message: "
          + urlConnection.getResponseMessage()
          + ", body: "
          + message);
    }
    urlConnection.disconnect();
  }

  ProjectSettings fetchSettings() throws IOException {
    HttpsURLConnection urlConnection =
        (HttpsURLConnection) createUrl("project/" + writeKey + "/settings").openConnection();

    urlConnection.setDoInput(true);
    urlConnection.setRequestMethod("GET");
    urlConnection.setRequestProperty("Content-Type", "application/json");

    int responseCode = urlConnection.getResponseCode();
    InputStream in;
    if (responseCode == HTTP_OK) {
      in = new BufferedInputStream(urlConnection.getInputStream());
    } else {
      String message = readFully(urlConnection.getErrorStream());
      throw new IOException("Could not fetch settings. Response code: "
          + responseCode
          + ", response message: "
          + urlConnection.getResponseMessage()
          + ", body: "
          + message);
    }

    String json = readFully(in);
    in.close();
    urlConnection.disconnect();
    return ProjectSettings.create(json, System.currentTimeMillis());
  }
}
