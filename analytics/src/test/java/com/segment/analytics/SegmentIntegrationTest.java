/**
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

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.SegmentIntegration.MAX_QUEUE_SIZE;
import static com.segment.analytics.SegmentIntegration.UTF_8;
import static com.segment.analytics.TestUtils.SynchronousExecutor;
import static com.segment.analytics.TestUtils.TRACK_PAYLOAD;
import static com.segment.analytics.TestUtils.TRACK_PAYLOAD_JSON;
import static com.segment.analytics.TestUtils.mockApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.segment.analytics.Client.Connection;
import com.segment.analytics.PayloadQueue.PersistentQueue;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.TrackPayload;
import com.segment.analytics.internal.Utils;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SegmentIntegrationTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();
  QueueFile queueFile;

  private static Client.Connection mockConnection() {
    return mockConnection(mock(HttpURLConnection.class));
  }

  private static Client.Connection mockConnection(HttpURLConnection connection) {
    return new Client.Connection(connection, mock(InputStream.class), mock(OutputStream.class)) {
      @Override
      public void close() throws IOException {
        super.close();
      }
    };
  }

  @Before
  public void setUp() throws IOException {
    queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
  }

  @After
  public void tearDown() {
    assertThat(ShadowLog.getLogs()).isEmpty();
  }

  @Test
  public void enqueueAddsToQueueFile() throws IOException {
    PayloadQueue payloadQueue = new PersistentQueue(queueFile);
    SegmentIntegration segmentIntegration = new SegmentBuilder().payloadQueue(payloadQueue).build();
    segmentIntegration.performEnqueue(TRACK_PAYLOAD);
    assertThat(payloadQueue.size()).isEqualTo(1);
  }

  @Test
  public void enqueueWritesIntegrations() throws IOException {
    final HashMap<String, Boolean> integrations = new LinkedHashMap<>();
    integrations.put("All", false); // should overwrite existing values in the map.
    integrations.put("Segment.io", false); // should ignore Segment setting in payload.
    integrations.put("foo", true); // should add new values.
    PayloadQueue payloadQueue = mock(PayloadQueue.class);
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .payloadQueue(payloadQueue)
            .integrations(integrations)
            .build();

    TrackPayload trackPayload =
        new TrackPayload.Builder()
            .messageId("a161304c-498c-4830-9291-fcfb8498877b")
            .timestamp(Utils.parseISO8601Date("2014-12-15T13:32:44-0700"))
            .event("foo")
            .userId("userId")
            .build();

    segmentIntegration.performEnqueue(trackPayload);

    String expected =
        "{"
            + "\"channel\":\"mobile\","
            + "\"type\":\"track\","
            + "\"messageId\":\"a161304c-498c-4830-9291-fcfb8498877b\","
            + "\"timestamp\":\"2014-12-15T20:32:44.000Z\","
            + "\"context\":{},"
            + "\"integrations\":{\"All\":false,\"foo\":true},"
            + "\"userId\":\"userId\","
            + "\"anonymousId\":null,"
            + "\"event\":\"foo\","
            + "\"properties\":{}"
            + "}";
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(payloadQueue).add(captor.capture());
    String got = new String(captor.getValue(), UTF_8);
    assertThat(got).isEqualTo(expected);
  }

  @Test
  public void enqueueLimitsQueueSize() throws IOException {
    PayloadQueue payloadQueue = mock(PayloadQueue.class);
    // We want to trigger a remove, but not a flush.
    when(payloadQueue.size()).thenReturn(0, MAX_QUEUE_SIZE, MAX_QUEUE_SIZE, 0);
    SegmentIntegration segmentIntegration = new SegmentBuilder().payloadQueue(payloadQueue).build();

    segmentIntegration.performEnqueue(TRACK_PAYLOAD);

    verify(payloadQueue).remove(1); // Oldest entry is removed.
    verify(payloadQueue).add(any(byte[].class)); // Newest entry is added.
  }

  @Test
  public void exceptionIgnoredIfFailedToRemove() throws IOException {
    PayloadQueue payloadQueue = mock(PayloadQueue.class);
    doThrow(new IOException("no remove for you.")).when(payloadQueue).remove(1);
    when(payloadQueue.size()).thenReturn(MAX_QUEUE_SIZE); // trigger a remove
    SegmentIntegration segmentIntegration = new SegmentBuilder().payloadQueue(payloadQueue).build();

    try {
      segmentIntegration.performEnqueue(TRACK_PAYLOAD);
    } catch (IOError unexpected) {
      fail("did not expect QueueFile to throw an error.");
    }

    verify(payloadQueue, never()).add(any(byte[].class));
  }

  @Test
  public void enqueueMaxTriggersFlush() throws IOException {
    PayloadQueue payloadQueue = new PayloadQueue.PersistentQueue(queueFile);
    Client client = mock(Client.class);
    Client.Connection connection = mockConnection();
    when(client.upload()).thenReturn(connection);
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .client(client)
            .flushSize(5)
            .payloadQueue(payloadQueue)
            .build();

    for (int i = 0; i < 4; i++) {
      segmentIntegration.performEnqueue(TRACK_PAYLOAD);
    }
    verifyZeroInteractions(client);
    // Only the last enqueue should trigger an upload.
    segmentIntegration.performEnqueue(TRACK_PAYLOAD);

    verify(client).upload();
  }

  @Test
  public void flushRemovesItemsFromQueue() throws IOException {
    PayloadQueue payloadQueue = new PayloadQueue.PersistentQueue(queueFile);
    Client client = mock(Client.class);
    when(client.upload()).thenReturn(mockConnection());
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .client(client)
            .payloadQueue(payloadQueue)
            .build();
    byte[] bytes = TRACK_PAYLOAD_JSON.getBytes();
    for (int i = 0; i < 4; i++) {
      queueFile.add(bytes);
    }

    segmentIntegration.submitFlush();

    assertThat(queueFile.size()).isEqualTo(0);
  }

  @Test
  public void flushSubmitsToExecutor() throws IOException {
    ExecutorService executor = spy(new SynchronousExecutor());
    PayloadQueue payloadQueue = mock(PayloadQueue.class);
    when(payloadQueue.size()).thenReturn(1);
    SegmentIntegration dispatcher =
        new SegmentBuilder() //
            .payloadQueue(payloadQueue)
            .networkExecutor(executor)
            .build();

    dispatcher.submitFlush();

    verify(executor).submit(any(Runnable.class));
  }

  @Test
  public void flushChecksIfExecutorIsShutdownFirst() {
    ExecutorService executor = spy(new SynchronousExecutor());
    PayloadQueue payloadQueue = mock(PayloadQueue.class);
    when(payloadQueue.size()).thenReturn(1);
    SegmentIntegration dispatcher =
        new SegmentBuilder() //
            .payloadQueue(payloadQueue)
            .networkExecutor(executor)
            .build();

    dispatcher.shutdown();
    executor.shutdown();
    dispatcher.submitFlush();

    verify(executor, never()).submit(any(Runnable.class));
  }

  @Test
  public void flushWhenDisconnectedSkipsUpload() throws IOException {
    NetworkInfo networkInfo = mock(NetworkInfo.class);
    when(networkInfo.isConnectedOrConnecting()).thenReturn(false);
    ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
    when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    Context context = mockApplication();
    when(context.getSystemService(CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
    Client client = mock(Client.class);
    SegmentIntegration segmentIntegration =
        new SegmentBuilder().context(context).client(client).build();

    segmentIntegration.submitFlush();

    verify(client, never()).upload();
  }

  @Test
  public void flushWhenQueueSizeIsLessThanOneSkipsUpload() throws IOException {
    PayloadQueue payloadQueue = mock(PayloadQueue.class);
    when(payloadQueue.size()).thenReturn(0);
    Context context = mockApplication();
    Client client = mock(Client.class);
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .payloadQueue(payloadQueue)
            .context(context)
            .client(client)
            .build();

    segmentIntegration.submitFlush();

    verifyZeroInteractions(context);
    verify(client, never()).upload();
  }

  @Test
  public void flushDisconnectsConnection() throws IOException {
    Client client = mock(Client.class);
    PayloadQueue payloadQueue = new PayloadQueue.PersistentQueue(queueFile);
    queueFile.add(TRACK_PAYLOAD_JSON.getBytes());
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    Client.Connection connection = mockConnection(urlConnection);
    when(client.upload()).thenReturn(connection);
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .client(client) //
            .payloadQueue(payloadQueue) //
            .build();

    segmentIntegration.submitFlush();

    verify(urlConnection, times(2)).disconnect();
  }

  @Test
  public void removesRejectedPayloads() throws IOException {
    // todo: rewrite using mockwebserver.
    PayloadQueue payloadQueue = new PayloadQueue.PersistentQueue(queueFile);
    Client client = mock(Client.class);
    when(client.upload())
        .thenReturn(
            new Connection(
                mock(HttpURLConnection.class), mock(InputStream.class), mock(OutputStream.class)) {
              @Override
              public void close() throws IOException {
                throw new Client.HTTPException(400, "Bad Request", "bad request");
              }
            });
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .client(client)
            .payloadQueue(payloadQueue)
            .build();
    for (int i = 0; i < 4; i++) {
      payloadQueue.add(TRACK_PAYLOAD_JSON.getBytes());
    }

    segmentIntegration.submitFlush();

    assertThat(queueFile.size()).isEqualTo(0);
    verify(client).upload();
  }

  @Test
  public void ignoresServerError() throws IOException {
    // todo: rewrite using mockwebserver.
    PayloadQueue payloadQueue = new PayloadQueue.PersistentQueue(queueFile);
    Client client = mock(Client.class);
    when(client.upload())
        .thenReturn(
            new Connection(
                mock(HttpURLConnection.class), mock(InputStream.class), mock(OutputStream.class)) {
              @Override
              public void close() throws IOException {
                throw new Client.HTTPException(
                    500, "Internal Server Error", "internal server error");
              }
            });
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .client(client)
            .payloadQueue(payloadQueue)
            .build();
    for (int i = 0; i < 4; i++) {
      payloadQueue.add(TRACK_PAYLOAD_JSON.getBytes());
    }

    segmentIntegration.submitFlush();

    assertThat(queueFile.size()).isEqualTo(4);
    verify(client).upload();
  }

  @Test
  public void ignoresHTTP429Error() throws IOException {
    // todo: rewrite using mockwebserver.
    PayloadQueue payloadQueue = new PayloadQueue.PersistentQueue(queueFile);
    Client client = mock(Client.class);
    when(client.upload())
        .thenReturn(
            new Connection(
                mock(HttpURLConnection.class), mock(InputStream.class), mock(OutputStream.class)) {
              @Override
              public void close() throws IOException {
                throw new Client.HTTPException(429, "Too Many Requests", "too many requests");
              }
            });
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .client(client)
            .payloadQueue(payloadQueue)
            .build();
    for (int i = 0; i < 4; i++) {
      payloadQueue.add(TRACK_PAYLOAD_JSON.getBytes());
    }

    segmentIntegration.submitFlush();

    // Verify that messages were not removed from the queue when server returned a 429.
    assertThat(queueFile.size()).isEqualTo(4);
    verify(client).upload();
  }

  @Test
  public void serializationErrorSkipsAddingPayload() throws IOException {
    PayloadQueue payloadQueue = mock(PayloadQueue.class);
    Cartographer cartographer = mock(Cartographer.class);
    TrackPayload payload = new TrackPayload.Builder().event("event").userId("userId").build();
    SegmentIntegration segmentIntegration =
        new SegmentBuilder() //
            .cartographer(cartographer)
            .payloadQueue(payloadQueue)
            .build();

    // Serialized json is null.
    when(cartographer.toJson(anyMap())).thenReturn(null);
    segmentIntegration.performEnqueue(payload);
    verify(payloadQueue, never()).add((byte[]) any());

    // Serialized json is empty.
    when(cartographer.toJson(anyMap())).thenReturn("");
    segmentIntegration.performEnqueue(payload);
    verify(payloadQueue, never()).add((byte[]) any());

    // Serialized json is too large (> MAX_PAYLOAD_SIZE).
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < SegmentIntegration.MAX_PAYLOAD_SIZE + 1; i++) {
      stringBuilder.append("a");
    }
    when(cartographer.toJson(anyMap())).thenReturn(stringBuilder.toString());
    segmentIntegration.performEnqueue(payload);
    verify(payloadQueue, never()).add((byte[]) any());
  }

  @Test
  public void shutdown() throws IOException {
    PayloadQueue payloadQueue = mock(PayloadQueue.class);
    SegmentIntegration segmentIntegration = new SegmentBuilder().payloadQueue(payloadQueue).build();

    segmentIntegration.shutdown();

    verify(payloadQueue).close();
  }

  @Test
  public void payloadVisitorReadsOnly475KB() throws IOException {
    SegmentIntegration.PayloadWriter payloadWriter =
        new SegmentIntegration.PayloadWriter(
            mock(SegmentIntegration.BatchPayloadWriter.class), Crypto.none());
    byte[] bytes =
        ("{\n"
                + "        \"context\": {\n"
                + "          \"library\": \"analytics-android\",\n"
                + "          \"libraryVersion\": \"0.4.4\",\n"
                + "          \"telephony\": {\n"
                + "            \"radio\": \"gsm\",\n"
                + "            \"carrier\": \"FI elisa\"\n"
                + "          },\n"
                + "          \"wifi\": {\n"
                + "            \"connected\": false,\n"
                + "            \"available\": false\n"
                + "          },\n"
                + "          \"providers\": {\n"
                + "            \"Tapstream\": false,\n"
                + "            \"Amplitude\": false,\n"
                + "            \"Localytics\": false,\n"
                + "            \"Flurry\": false,\n"
                + "            \"Countly\": false,\n"
                + "            \"Bugsnag\": false,\n"
                + "            \"Quantcast\": false,\n"
                + "            \"Crittercism\": false,\n"
                + "            \"Google Analytics\": false,\n"
                + "            \"Omniture\": false,\n"
                + "            \"Mixpanel\": false\n"
                + "          },\n"
                + "          \"location\": {\n"
                + "            \"speed\": 0,\n"
                + "            \"longitude\": 24.937207,\n"
                + "            \"latitude\": 60.2495497\n"
                + "          },\n"
                + "          \"locale\": {\n"
                + "            \"carrier\": \"FI elisa\",\n"
                + "            \"language\": \"English\",\n"
                + "            \"country\": \"United States\"\n"
                + "          },\n"
                + "          \"device\": {\n"
                + "            \"userId\": \"123\",\n"
                + "            \"brand\": \"samsung\",\n"
                + "            \"release\": \"4.2.2\",\n"
                + "            \"manufacturer\": \"samsung\",\n"
                + "            \"sdk\": 17\n"
                + "          },\n"
                + "          \"display\": {\n"
                + "            \"density\": 1.5,\n"
                + "            \"width\": 800,\n"
                + "            \"height\": 480\n"
                + "          },\n"
                + "          \"build\": {\n"
                + "            \"name\": \"1.0\",\n"
                + "            \"code\": 1\n"
                + "          },\n"
                + "          \"ip\": \"80.186.195.102\",\n"
                + "          \"inferredIp\": true\n"
                + "        }\n"
                + "      }")
            .getBytes(); // length 1432
    // Fill the payload with (1432 * 500) = ~716kb of data
    for (int i = 0; i < 500; i++) {
      queueFile.add(bytes);
    }

    queueFile.forEach(payloadWriter);

    // Verify only (331 * 1432) = 473992 < 475KB bytes are read
    assertThat(payloadWriter.payloadCount).isEqualTo(331);
  }

  private static class SegmentBuilder {

    Client client;
    Stats stats;
    PayloadQueue payloadQueue;
    Context context;
    Cartographer cartographer;
    Map<String, Boolean> integrations;
    int flushInterval = Utils.DEFAULT_FLUSH_INTERVAL;
    int flushSize = Utils.DEFAULT_FLUSH_QUEUE_SIZE;
    Logger logger = Logger.with(NONE);
    ExecutorService networkExecutor;

    SegmentBuilder() {
      initMocks(this);
      context = mockApplication();
      when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)) //
          .thenReturn(PERMISSION_DENIED);
      cartographer = Cartographer.INSTANCE;
    }

    public SegmentBuilder client(Client client) {
      this.client = client;
      return this;
    }

    public SegmentBuilder stats(Stats stats) {
      this.stats = stats;
      return this;
    }

    public SegmentBuilder payloadQueue(PayloadQueue payloadQueue) {
      this.payloadQueue = payloadQueue;
      return this;
    }

    public SegmentBuilder context(Context context) {
      this.context = context;
      return this;
    }

    public SegmentBuilder cartographer(Cartographer cartographer) {
      this.cartographer = cartographer;
      return this;
    }

    public SegmentBuilder integrations(Map<String, Boolean> integrations) {
      this.integrations = integrations;
      return this;
    }

    public SegmentBuilder flushInterval(int flushInterval) {
      this.flushInterval = flushInterval;
      return this;
    }

    public SegmentBuilder flushSize(int flushSize) {
      this.flushSize = flushSize;
      return this;
    }

    public SegmentBuilder log(Logger logger) {
      this.logger = logger;
      return this;
    }

    public SegmentBuilder networkExecutor(ExecutorService networkExecutor) {
      this.networkExecutor = networkExecutor;
      return this;
    }

    SegmentIntegration build() {
      if (context == null) {
        context = mockApplication();
        when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)) //
            .thenReturn(PERMISSION_DENIED);
      }
      if (client == null) {
        client = mock(Client.class);
      }
      if (cartographer == null) {
        cartographer = Cartographer.INSTANCE;
      }
      if (payloadQueue == null) {
        payloadQueue = mock(PayloadQueue.class);
      }
      if (stats == null) {
        stats = mock(Stats.class);
      }
      if (integrations == null) {
        integrations = Collections.emptyMap();
      }
      if (networkExecutor == null) {
        networkExecutor = new SynchronousExecutor();
      }
      return new SegmentIntegration(
          context,
          client,
          cartographer,
          networkExecutor,
          payloadQueue,
          stats,
          integrations,
          flushInterval,
          flushSize,
          logger,
          Crypto.none());
    }
  }
}
