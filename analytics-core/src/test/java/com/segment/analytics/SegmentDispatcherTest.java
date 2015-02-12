package com.segment.analytics;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.segment.analytics.internal.Utils;
import com.segment.analytics.internal.model.payloads.BasePayload;
import edu.emory.mathcs.backport.java.util.Collections;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.SegmentDispatcher.MAX_QUEUE_SIZE;
import static com.segment.analytics.TestUtils.mockApplication;
import static com.segment.analytics.internal.Utils.toISO8601Date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class SegmentDispatcherTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private static Client.Connection mockConnection() {
    return mockConnection(mock(HttpURLConnection.class));
  }

  private static Client.Connection mockConnection(HttpURLConnection connection) {
    return new Client.Connection(connection, mock(InputStream.class), mock(OutputStream.class)) {
      @Override public void close() throws IOException {
        super.close();
      }
    };
  }

  @After public void tearDown() {
    assertThat(ShadowLog.getLogs()).isEmpty();
  }

  @Test public void enqueueAddsToQueue() throws IOException {
    QueueFile queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
    SegmentDispatcher segmentDispatcher = new SegmentBuilder().queueFile(queueFile).build();
    BasePayload payload = mock(BasePayload.class);

    segmentDispatcher.performEnqueue(payload);
    segmentDispatcher.performEnqueue(payload);

    assertThat(segmentDispatcher.queueFile.size()).isEqualTo(2);
    assertThat(segmentDispatcher.queueFile.peek()).isEqualTo("{}".getBytes());
  }

  @Test public void enqueueLimitsQueueSize() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    when(queueFile.size()).thenReturn(MAX_QUEUE_SIZE, 0); // we don't need to trigger a real flush
    SegmentDispatcher segmentDispatcher = new SegmentBuilder().queueFile(queueFile).build();
    BasePayload payload = mock(BasePayload.class);

    segmentDispatcher.performEnqueue(payload);

    verify(queueFile).remove(); // oldest entry is removed
    verify(queueFile).add("{}".getBytes()); // newest entry is added
  }

  @Test public void exceptionThrownIfFailedToRemove() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    doThrow(new IOException("mock")).when(queueFile).remove();
    when(queueFile.size()).thenReturn(MAX_QUEUE_SIZE);
    SegmentDispatcher segmentDispatcher = new SegmentBuilder().queueFile(queueFile).build();
    BasePayload payload = mock(BasePayload.class);

    try {
      segmentDispatcher.performEnqueue(payload);
      fail("Expected QueueFile to throw an error.");
    } catch (RuntimeException expected) {
      assertThat(expected).hasMessage("Could not remove payload from queue.");
      assertThat(expected.getCause()).hasMessage("mock").isOfAnyClassIn(IOException.class);
    }
  }

  @Test public void enqueueMaxTriggersFlush() throws IOException {
    QueueFile queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
    Client client = mock(Client.class);
    Client.Connection connection = mockConnection();
    when(client.upload()).thenReturn(connection);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().client(client).flushSize(5).queueFile(queueFile).build();
    BasePayload payload = mock(BasePayload.class);

    for (int i = 0; i < 5; i++) {
      segmentDispatcher.performEnqueue(payload);
    }

    verify(client).upload();
  }

  @Test public void flushRemovesItemsFromQueue() throws IOException {
    QueueFile queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
    BasePayload payload = mock(BasePayload.class);
    Client client = mock(Client.class);
    Client.Connection connection = mockConnection();
    when(client.upload()).thenReturn(connection);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().client(client).queueFile(queueFile).build();

    for (int i = 0; i < 5; i++) {
      segmentDispatcher.performEnqueue(payload);
    }
    segmentDispatcher.performFlush();

    assertThat(queueFile.size()).isEqualTo(0);
  }

  @Test public void flushingWhenDisconnectedSkipsUpload() throws IOException {
    Context context = mockApplication();
    ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
    NetworkInfo networkInfo = mock(NetworkInfo.class);
    Client client = mock(Client.class);

    when(context.getSystemService(CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
    when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    when(networkInfo.isConnectedOrConnecting()).thenReturn(false);
    SegmentDispatcher segmentDispatcher = new SegmentBuilder().context(context).build();

    segmentDispatcher.performFlush();

    verify(client, never()).upload();
  }

  @Test public void flushClosesConnection() throws IOException {
    Client client = mock(Client.class);
    QueueFile queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
    queueFile.add("{}".getBytes());
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    Client.Connection connection = mockConnection(urlConnection);
    when(client.upload()).thenReturn(connection);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().client(client).queueFile(queueFile).build();

    segmentDispatcher.performFlush();

    verify(urlConnection).disconnect();
  }

  @Test public void serializationErrorSkipsPayload() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    Cartographer cartographer = mock(Cartographer.class);
    BasePayload payload = mock(BasePayload.class);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().cartographer(cartographer).queueFile(queueFile).build();

    when(cartographer.toJson(anyMap())).thenReturn(null);
    segmentDispatcher.performEnqueue(payload);
    verify(queueFile, never()).add((byte[]) any());

    when(cartographer.toJson(anyMap())).thenReturn("");
    segmentDispatcher.performEnqueue(payload);
    verify(queueFile, never()).add((byte[]) any());

    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 10; i < 14000; i++) {
      stringBuilder.append(UUID.randomUUID().toString());
    }
    when(cartographer.toJson(anyMap())).thenReturn(stringBuilder.toString());
    segmentDispatcher.performEnqueue(payload);
    verify(queueFile, never()).add((byte[]) any());

    doThrow(new IOException("mock")).when(cartographer).toJson(anyMap());
    segmentDispatcher.performEnqueue(payload);
    verify(queueFile, never()).add((byte[]) any());
  }

  @Test public void shutdown() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    SegmentDispatcher segmentDispatcher = new SegmentBuilder().queueFile(queueFile).build();

    segmentDispatcher.shutdown();

    verify(queueFile).close();
  }

  @Test public void batchPayloadWriter() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentDispatcher.BatchPayloadWriter batchPayloadWriter =
        new SegmentDispatcher.BatchPayloadWriter(byteArrayOutputStream);

    final HashMap<String, Boolean> integrations = new LinkedHashMap<>();
    integrations.put("foo", false);
    integrations.put("bar", true);

    batchPayloadWriter.beginObject()
        .integrations(integrations)
        .beginBatchArray()
        .emitPayloadObject("foobarbazqux")
        .emitPayloadObject("{}")
        .emitPayloadObject("2")
        .endBatchArray()
        .endObject()
        .close();

    assertThat(byteArrayOutputStream.toString()) //
        .overridingErrorMessage("It's OK if this failed close to midnight!")
        .isEqualTo("{\"integrations\":{\"foo\":false,\"bar\":true},"
            + "\"batch\":[foobarbazqux,{},2],"
            + "\"sentAt\":\""
            + toISO8601Date(new Date())
            + "\"}");
  }

  @Test public void batchPayloadWriterSingleItem() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentDispatcher.BatchPayloadWriter batchPayloadWriter =
        new SegmentDispatcher.BatchPayloadWriter(byteArrayOutputStream);

    final HashMap<String, Boolean> integrations = new LinkedHashMap<>();
    integrations.put("foo", false);
    integrations.put("bar", true);

    batchPayloadWriter.beginObject()
        .integrations(integrations)
        .beginBatchArray()
        .emitPayloadObject("qaz")
        .endBatchArray()
        .endObject()
        .close();

    assertThat(byteArrayOutputStream.toString()) //
        .isEqualTo("{\"integrations\":{\"foo\":false,\"bar\":true},\"batch\":[qaz],\"sentAt\":\""
            + toISO8601Date(new Date())
            + "\"}").overridingErrorMessage("its ok if this failed close to midnight!");
  }

  @Test public void batchPayloadWriterNoIntegrations() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentDispatcher.BatchPayloadWriter batchPayloadWriter =
        new SegmentDispatcher.BatchPayloadWriter(byteArrayOutputStream);

    batchPayloadWriter.beginObject()
        .integrations(Collections.emptyMap())
        .beginBatchArray()
        .emitPayloadObject("foo")
        .endBatchArray()
        .endObject()
        .close();

    assertThat(byteArrayOutputStream.toString()) //
        .isEqualTo("{\"batch\":[foo],\"sentAt\":\"" + toISO8601Date(new Date()) + "\"}")
        .overridingErrorMessage("its ok if this failed close to midnight!");
  }

  @Test public void batchPayloadWriterFailsForNoItem() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentDispatcher.BatchPayloadWriter batchPayloadWriter =
        new SegmentDispatcher.BatchPayloadWriter(byteArrayOutputStream);

    HashMap<String, Boolean> integrations = new LinkedHashMap<>();
    integrations.put("foo", false);
    integrations.put("bar", true);

    try {
      batchPayloadWriter.beginObject()
          .integrations(integrations)
          .beginBatchArray()
          .endBatchArray()
          .endObject()
          .close();
    } catch (IOException exception) {
      assertThat(exception).hasMessage("At least one payload must be provided.");
    }
  }

  @Test public void payloadVisitorReadsOnly450KB() throws IOException {
    SegmentDispatcher.PayloadWriter payloadWriter =
        new SegmentDispatcher.PayloadWriter(mock(SegmentDispatcher.BatchPayloadWriter.class));
    byte[] bytes = ("{\n"
        + "        'context': {\n"
        + "          'library': 'analytics-android',\n"
        + "          'libraryVersion': '0.4.4',\n"
        + "          'telephony': {\n"
        + "            'radio': 'gsm',\n"
        + "            'carrier': 'FI elisa'\n"
        + "          },\n"
        + "          'wifi': {\n"
        + "            'connected': false,\n"
        + "            'available': false\n"
        + "          },\n"
        + "          'providers': {\n"
        + "            'Tapstream': false,\n"
        + "            'Amplitude': false,\n"
        + "            'Localytics': false,\n"
        + "            'Flurry': false,\n"
        + "            'Countly': false,\n"
        + "            'Bugsnag': false,\n"
        + "            'Quantcast': false,\n"
        + "            'Crittercism': false,\n"
        + "            'Google Analytics': false,\n"
        + "            'Omniture': false,\n"
        + "            'Mixpanel': false\n"
        + "          },\n"
        + "          'location': {\n"
        + "            'speed': 0,\n"
        + "            'longitude': 24.937207,\n"
        + "            'latitude': 60.2495497\n"
        + "          },\n"
        + "          'locale': {\n"
        + "            'carrier': 'FI elisa',\n"
        + "            'language': 'English',\n"
        + "            'country': 'United States'\n"
        + "          },\n"
        + "          'device': {\n"
        + "            'userId': '123',\n"
        + "            'brand': 'samsung',\n"
        + "            'release': '4.2.2',\n"
        + "            'manufacturer': 'samsung',\n"
        + "            'sdk': 17\n"
        + "          },\n"
        + "          'display': {\n"
        + "            'density': 1.5,\n"
        + "            'width': 800,\n"
        + "            'height': 480\n"
        + "          },\n"
        + "          'build': {\n"
        + "            'name': '1.0',\n"
        + "            'code': 1\n"
        + "          },\n"
        + "          'ip': '80.186.195.102',\n"
        + "          'inferredIp': true\n"
        + "        }\n"
        + "      }").getBytes(); // length 1432
    QueueFile queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
    // Fill the fill to ~716kb of payload
    for (int i = 0; i < 500; i++) {
      queueFile.add(bytes);
    }

    queueFile.forEach(payloadWriter);

    // Verify only (314 * 1432) = 449648 < 500kb bytes are read
    assertThat(payloadWriter.payloadCount).isEqualTo(314);
  }

  private static class SegmentBuilder {
    Client client;
    Stats stats;
    QueueFile queueFile;
    Context context;
    Cartographer cartographer;
    Map<String, Boolean> integrations;
    int flushInterval = Utils.DEFAULT_FLUSH_INTERVAL;
    int flushSize = Utils.DEFAULT_FLUSH_QUEUE_SIZE;
    Analytics.LogLevel logLevel = NONE;

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

    public SegmentBuilder queueFile(QueueFile queueFile) {
      this.queueFile = queueFile;
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

    public SegmentBuilder logLevel(Analytics.LogLevel logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    SegmentDispatcher build() {
      if (context == null) {
        context = mockApplication();
        when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)) //
            .thenReturn(PERMISSION_DENIED);
      }
      if (client == null) client = mock(Client.class);
      if (cartographer == null) cartographer = Cartographer.INSTANCE;
      if (queueFile == null) queueFile = mock(QueueFile.class);
      if (stats == null) stats = mock(Stats.class);
      if (integrations == null) integrations = Collections.emptyMap();
      return new SegmentDispatcher(context, client, cartographer, queueFile, stats, integrations,
          flushInterval, flushSize, logLevel);
    }
  }
}
