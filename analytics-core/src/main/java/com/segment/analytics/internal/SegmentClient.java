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

package com.segment.analytics.internal;

import android.content.Context;
import android.util.Base64;
import com.segment.analytics.internal.model.ProjectSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

import static com.segment.analytics.internal.Utils.buffer;
import static java.net.HttpURLConnection.HTTP_OK;

public class SegmentClient {
  private static final String API_URL = "https://api.segment.io/";

  final String writeKey;
  final HttpURLConnectionFactory connectionFactory;
  final Context context;
  final Cartographer cartographer;

  public SegmentClient(Context context, Cartographer cartographer, String writeKey) {
    this(context, cartographer, HttpURLConnectionFactory.DEFAULT, writeKey);
  }

  SegmentClient(Context context, Cartographer cartographer,
      HttpURLConnectionFactory connectionFactory, String writeKey) {
    this.context = context;
    this.cartographer = cartographer;
    this.connectionFactory = connectionFactory;
    this.writeKey = writeKey;
  }

  /** Consume the InputStream and read it into a string. */
  private static String readFully(InputStream is) throws IOException {
    BufferedReader reader = buffer(is);
    StringBuilder response = new StringBuilder();
    try {
      for (String line; (line = reader.readLine()) != null; ) {
        response.append(line);
      }
    } finally {
      reader.close();
    }
    return response.toString();
  }

  void upload(final StreamWriter streamWriter) throws IOException {
    HttpsURLConnection connection = connectionFactory.open(API_URL + "v1/import");
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("Authorization",
        "Basic " + Base64.encodeToString((writeKey + ":").getBytes(), Base64.NO_WRAP));
    connection.setChunkedStreamingMode(0);

    OutputStream outputStream = connection.getOutputStream();
    try {
      streamWriter.write(outputStream);
    } finally {
      outputStream.close();
    }

    try {
      int responseCode = connection.getResponseCode();
      if (responseCode != HTTP_OK) {
        // todo: throw a different exception if it's an unretryable error, e.g. invalid payload
        InputStream errorStream = connection.getErrorStream();
        String body = (errorStream == null) ? null : readFully(errorStream);
        throw new IOException("Could not upload payloads. Response code: "
            + responseCode
            + ", response message: "
            + connection.getResponseMessage()
            + ", body: "
            + body);
      }
    } finally {
      connection.disconnect();
    }
  }

  ProjectSettings fetchSettings() throws IOException {
    HttpsURLConnection connection =
        connectionFactory.open(API_URL + "project/" + writeKey + "/settings");
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Content-Type", "application/json");

    try {
      int responseCode = connection.getResponseCode();
      if (responseCode == HTTP_OK) {
        Map<String, Object> map = cartographer.fromJson(buffer(connection.getInputStream()));
        ProjectSettings projectSettings = ProjectSettings.create(map, System.currentTimeMillis());

        for (String key : projectSettings.keySet()) {
          // todo: find out why we return Segment as a key in the API
          if (key.contains("Segment") || key.contains("timestamp")) continue;
          // todo: skip downloading for integrations that are already bundled

          String fileKey = key.replace(' ', '-').toLowerCase();
          String jarFile = fileKey + "-3.0.0-SNAPSHOT-jar-with-dependencies.jar";
          HttpsURLConnection jarConnection = connectionFactory //
              .open("https://dl.dropboxusercontent.com/u/11371156/integrations/" + jarFile);
          jarConnection.setRequestMethod("GET");
          jarConnection.connect();
          jarConnection.setInstanceFollowRedirects(true);
          File parent = context.getFilesDir();
          File directory = new File(parent, fileKey);
          if (directory.exists() || directory.mkdirs() || directory.isDirectory()) {
            File jar = new File(directory, jarFile);
            if (jar.exists()) jar.delete(); // todo: for testing, so we do clean downloads each time
            FileOutputStream fileOutput = new FileOutputStream(jar);
            InputStream inputStream = jarConnection.getInputStream();
            byte[] buffer = new byte[1024];
            try {
              for (int bufferLength; (bufferLength = inputStream.read(buffer)) > 0; ) {
                fileOutput.write(buffer, 0, bufferLength);
              }
            } finally {
              fileOutput.close();
              inputStream.close();
            }
          }
        }
        return projectSettings;
      } else {
        String message = readFully(connection.getErrorStream());
        throw new IOException("Could not fetch settings. Response code: "
            + responseCode
            + ", response message: "
            + connection.getResponseMessage()
            + ", body: "
            + message);
      }
    } finally {
      connection.disconnect();
    }
  }

  interface HttpURLConnectionFactory {
    HttpsURLConnection open(String endpoint) throws IOException;

    HttpURLConnectionFactory DEFAULT = new HttpURLConnectionFactory() {
      @Override public HttpsURLConnection open(String url) throws IOException {
        return (HttpsURLConnection) new URL(url).openConnection();
      }
    };
  }

  /** An entity that writes to an {@link OutputStream}. */
  interface StreamWriter {
    void write(OutputStream outputStream) throws IOException;
  }
}
