package com.segment.analytics;

import android.util.Base64;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Abstraction to customize how connections are created. This is can be used to point our SDK at
 * your proxy server for instance.
 */
public class ConnectionFactory {
  private static final int DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
  private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s

  private String authorizationHeader(String writeKey) {
    return "Basic " + Base64.encodeToString((writeKey + ":").getBytes(), Base64.NO_WRAP);
  }

  /** Return a {@link HttpURLConnection} that reads JSON formatted project settings. */
  public HttpURLConnection projectSettings(String writeKey) throws IOException {
    HttpURLConnection connection =
        openConnection("http://cdn.segment.com/v1/projects/" + writeKey + "/settings");
    connection.setRequestProperty("Content-Type", "application/json");
    return connection;
  }

  /**
   * Return a {@link HttpURLConnection} that writes batched payloads to {@code
   * https://api.segment.io/v1/import}.
   */
  public HttpURLConnection upload(String writeKey) throws IOException {
    HttpURLConnection connection = openConnection("https://api.segment.io/v1/import");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("Authorization", authorizationHeader(writeKey));
    connection.setDoOutput(true);
    connection.setChunkedStreamingMode(0);
    return connection;
  }

  /**
   * Configures defaults for connections opened with both {@link #upload(String)} and {@link
   * #projectSettings(String)}.
   */
  protected HttpURLConnection openConnection(String url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
    connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
    connection.setDoInput(true);
    return connection;
  }
}
