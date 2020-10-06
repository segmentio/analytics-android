package com.segment.analytics;

import android.util.Log;

import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import static com.segment.analytics.internal.Utils.getInputStream;
import static com.segment.analytics.internal.Utils.readFully;

public class WebhookIntegration extends Integration<Void> {

  public static class WebhookIntegrationFactory implements Factory {
    private String key, webhookUrl;

    public WebhookIntegrationFactory(String key, String webhookUrl) {
      this.key = key;
      this.webhookUrl = webhookUrl;
    }

    @Override
    public Integration<?> create(ValueMap settings, Analytics analytics) {
      return new WebhookIntegration(webhookUrl, analytics.tag, key(), analytics.networkExecutor);
    }

    @Override
    public String key() {
      return "webhook_" + key;
    }
  }

  private String webhookUrl, tag, key;
  private ExecutorService networkExecutor;

  public WebhookIntegration(String webhookUrl, String tag, String key, ExecutorService networkExecutor) {
    this.webhookUrl = webhookUrl;
    this.tag = tag;
    this.key = key;
    this.networkExecutor = networkExecutor;
  }

  /**
   * Sends a JSON payload to the specified webhookUrl, with the Content-Type=application/json
   * header set
   */
  private void sendPayloadToWebhook(ValueMap payload) {
    networkExecutor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          Log.d(tag, String.format("Running %s payload through %s", payload.getString("type"), key));
          URL requestedURL = null;
          try {
            requestedURL = new URL(webhookUrl);
          } catch (MalformedURLException e) {
            throw new IOException("Attempted to use malformed url: $webhookUrl", e);
          }

          HttpURLConnection connection = (HttpURLConnection) requestedURL.openConnection();
          connection.setDoOutput(true);
          connection.setChunkedStreamingMode(0);
          connection.setRequestProperty("Content-Type", "application/json");

          DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
          String payloadJson = payload.toJsonObject().toString();
          outputStream.writeBytes(payloadJson);

          try {
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300) {
              String responseBody;
              InputStream inputStream = null;
              try {
                inputStream = getInputStream(connection);
                responseBody = readFully(inputStream);
              } catch (IOException e) {
                responseBody =
                    "Could not read response body for rejected message: "
                        + e.toString();
              } finally {
                if (inputStream != null) {
                  inputStream.close();
                }
              }
              Log.w(tag, String.format("Failed to send payload, statusCode=%d, body=%s", responseCode, responseBody));
            }
          } finally {
            outputStream.close();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public void identify(IdentifyPayload identify) {
    sendPayloadToWebhook(identify);
  }

  public void group(GroupPayload group) {
    sendPayloadToWebhook(group);
  }

  @Override
  public void track(TrackPayload track) {
    sendPayloadToWebhook(track);
  }

  @Override
  public void alias(AliasPayload alias) {
    sendPayloadToWebhook(alias);
  }

  @Override
  public void screen(ScreenPayload screen) {
    sendPayloadToWebhook(screen);
  }
}
