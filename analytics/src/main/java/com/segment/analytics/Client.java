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

import android.text.TextUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.zip.GZIPOutputStream;

import static com.segment.analytics.internal.Utils.readFully;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * HTTP client which can upload payloads and fetch project settings from the Segment public API.
 */
class Client {

  final ConnectionFactory connectionFactory;
  final String writeKey;

  private static Connection createPostConnection(HttpURLConnection connection) throws IOException {
    final OutputStream outputStream;
    // Clients may have opted out of gzip compression via a custom connection factory.
    String contentEncoding = connection.getRequestProperty("Content-Encoding");
    if (TextUtils.equals("gzip", contentEncoding)) {
      outputStream = new GZIPOutputStream(connection.getOutputStream());
    } else {
      outputStream = connection.getOutputStream();
    }
    return new Connection(connection, null, outputStream) {
      @Override public void close() throws IOException {
        try {
          int responseCode = connection.getResponseCode();
          if (responseCode >= 300) {
            String responseBody;
            try {
              responseBody = readFully(connection.getInputStream());
            } catch (IOException e) {
              responseBody = "Could not read response body for rejected message: " + e.toString();
            }
            throw new HTTPException(responseCode, connection.getResponseMessage(), responseBody);
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

  Client(String writeKey, ConnectionFactory connectionFactory) {
    this.writeKey = writeKey;
    this.connectionFactory = connectionFactory;
  }

  Connection upload() throws IOException {
    HttpURLConnection connection = connectionFactory.upload(writeKey);
    return createPostConnection(connection);
  }

  Connection attribution() throws IOException {
    HttpURLConnection connection = connectionFactory.attribution(writeKey);
    return createPostConnection(connection);
  }

  Connection fetchSettings() throws IOException {
    HttpURLConnection connection = connectionFactory.projectSettings(writeKey);
    int responseCode = connection.getResponseCode();
    if (responseCode != HTTP_OK) {
      connection.disconnect();
      throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
    }
    return createGetConnection(connection);
  }

  /** Represents an HTTP exception thrown for unexpected/non 2xx response codes. */
  static class HTTPException extends IOException {
    final int responseCode;
    final String responseMessage;
    final String responseBody;

    HTTPException(int responseCode, String responseMessage, String responseBody) {
      super("HTTP " + responseCode + ": " + responseMessage + ". Response: " + responseBody);
      this.responseCode = responseCode;
      this.responseMessage = responseMessage;
      this.responseBody = responseBody;
    }
  }

  /**
   * Wraps an HTTP connection. Callers can either read from the connection via the {@link
   * InputStream} or write to the connection via {@link OutputStream}.
   */
  static abstract class Connection implements Closeable {
    final HttpURLConnection connection;
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
