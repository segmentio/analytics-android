/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment, Inc.
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

import android.content.Context;
import android.util.Base64;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * HTTP client which can upload payloads and fetch project settings from the Segment public API,
 * and
 * download a file.
 */
class Client {
  private static final String API_URL = "https://api.segment.io";
  private static final int DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
  private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s

  final String writeKey;
  final Context context;

  private static Connection createPostConnection(HttpURLConnection connection) throws IOException {
    return new Connection(connection, null, connection.getOutputStream()) {
      @Override public void close() throws IOException {
        try {
          int responseCode = connection.getResponseCode();
          if (responseCode != HTTP_OK) {
            throw new IOException(responseCode + " " + connection.getResponseMessage());
          }
        } finally {
          super.close();
          os.close();
        }
      }
    };
  }

  private static Connection createGetConnection(HttpURLConnection connection) throws IOException {
    return new Connection(connection, connection.getInputStream(), null) {
      @Override public void close() throws IOException {
        super.close();
        is.close();
      }
    };
  }

  private static String authorizationHeader(String writeKey) {
    return "Basic " + Base64.encodeToString((writeKey + ":").getBytes(), Base64.NO_WRAP);
  }

  Client(Context context, String writeKey) {
    this.context = context;
    this.writeKey = writeKey;
  }

  protected HttpURLConnection openConnection(String url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
    connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
    connection.setDoInput(true);
    return connection;
  }

  Connection upload() throws IOException {
    HttpURLConnection connection = openConnection(API_URL + "/v1/import");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("Authorization", authorizationHeader(writeKey));
    connection.setDoOutput(true);
    connection.setChunkedStreamingMode(0);
    return createPostConnection(connection);
  }

  Connection fetchSettings() throws IOException {
    HttpURLConnection connection = openConnection(API_URL + "/project/" + writeKey + "/settings");
    connection.setRequestProperty("Content-Type", "application/json");

    int responseCode = connection.getResponseCode();
    if (responseCode != HTTP_OK) {
      connection.disconnect();
      throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
    }
    return createGetConnection(connection);
  }

  void downloadFile(String url, File location) throws IOException {
    HttpURLConnection connection = openConnection(url);
    connection.setInstanceFollowRedirects(true);
    connection.connect();

    FileOutputStream fos = new FileOutputStream(location);
    InputStream is = connection.getInputStream();
    byte[] buffer = new byte[1024];
    try {
      for (int bufferLength; (bufferLength = is.read(buffer)) > 0; ) {
        fos.write(buffer, 0, bufferLength);
      }
    } finally {
      connection.disconnect();
      fos.close();
      is.close();
    }
  }

  /**
   * Wraps an HTTP connection. Callers can either read from the connection via the {@link
   * InputStream} or write to the connection via {@link OutputStream}.
   */
  static abstract class Connection implements Closeable {
    protected final HttpURLConnection connection;
    final InputStream is;
    final OutputStream os;

    Connection(HttpURLConnection connection, InputStream is, OutputStream os) {
      if (connection == null) {
        throw new IllegalArgumentException("connection == null");
      }
      this.connection = connection;
      this.is = is;
      this.os = os;
    }

    @Override public void close() throws IOException {
      connection.disconnect();
    }
  }
}
