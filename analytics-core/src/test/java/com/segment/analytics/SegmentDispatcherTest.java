package com.segment.analytics;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.segment.analytics.internal.Utils;
import com.segment.analytics.internal.model.payloads.BasePayload;
import edu.emory.mathcs.backport.java.util.Collections;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;
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
import static com.segment.analytics.SegmentDispatcher.SegmentDispatcherHandler.REQUEST_FLUSH;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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

  @Test public void enqueueAddsToQueueFile() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    SegmentDispatcher segmentDispatcher = new SegmentBuilder().queueFile(queueFile).build();

    segmentDispatcher.performEnqueue(TRACK_PAYLOAD);

    verify(queueFile).add(TRACK_PAYLOAD_JSON.getBytes());
  }

  @Test public void enqueueLimitsQueueSize() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    // we want to trigger a remove, but not a flush, so return 0 the second time size() is called
    when(queueFile.size()).thenReturn(MAX_QUEUE_SIZE, 0);
    SegmentDispatcher segmentDispatcher = new SegmentBuilder().queueFile(queueFile).build();

    segmentDispatcher.performEnqueue(TRACK_PAYLOAD);

    verify(queueFile).remove(); // oldest entry is removed
    verify(queueFile).add(TRACK_PAYLOAD_JSON.getBytes()); // newest entry is added
  }

  @Test public void exceptionThrownIfFailedToRemove() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    doThrow(new IOException("no remove for you.")).when(queueFile).remove();
    when(queueFile.size()).thenReturn(MAX_QUEUE_SIZE); // trigger a remove
    SegmentDispatcher segmentDispatcher = new SegmentBuilder().queueFile(queueFile).build();

    try {
      segmentDispatcher.performEnqueue(TRACK_PAYLOAD);
      fail("expected QueueFile to throw an error.");
    } catch (RuntimeException expected) {
      assertThat(expected).hasMessage("Could not remove payload from queue.");
      assertThat(expected.getCause()).hasMessage("no remove for you.")
          .isInstanceOf(IOException.class);
    }
  }

  @Test public void enqueueMaxTriggersFlush() throws IOException {
    QueueFile queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
    Client client = mock(Client.class);
    Client.Connection connection = mockConnection();
    when(client.upload()).thenReturn(connection);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().client(client).flushSize(5).queueFile(queueFile).build();

    for (int i = 0; i < 4; i++) {
      segmentDispatcher.performEnqueue(TRACK_PAYLOAD);
    }
    verifyZeroInteractions(client);
    // Only the last enqueue should trigger an upload.
    segmentDispatcher.performEnqueue(TRACK_PAYLOAD);

    verify(client).upload();
  }

  @Test public void flushRemovesItemsFromQueue() throws IOException {
    QueueFile queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
    Client client = mock(Client.class);
    when(client.upload()).thenReturn(mockConnection());
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().client(client).queueFile(queueFile).build();
    byte[] bytes = TRACK_PAYLOAD_JSON.getBytes();
    for (int i = 0; i < 4; i++) {
      queueFile.add(bytes);
    }

    segmentDispatcher.performFlush();

    assertThat(queueFile.size()).isEqualTo(0);
  }

  @Test public void flushWhenDisconnectedSkipsUpload() throws IOException {
    NetworkInfo networkInfo = mock(NetworkInfo.class);
    when(networkInfo.isConnectedOrConnecting()).thenReturn(false);
    ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
    when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    Context context = mockApplication();
    when(context.getSystemService(CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
    Client client = mock(Client.class);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().context(context).client(client).build();

    segmentDispatcher.performFlush();

    verify(client, never()).upload();
    assertThat(segmentDispatcher.handler.hasMessages(REQUEST_FLUSH)).isTrue();
  }

  @Test public void flushWhenQueueSizeIsLessThanOneSkipsUpload() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    when(queueFile.size()).thenReturn(0);
    Context context = mockApplication();
    Client client = mock(Client.class);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().queueFile(queueFile).context(context).client(client).build();

    segmentDispatcher.performFlush();

    verifyZeroInteractions(context);
    verify(client, never()).upload();
    assertThat(segmentDispatcher.handler.hasMessages(REQUEST_FLUSH)).isTrue();
  }

  @Test public void flushDisconnectsConnection() throws IOException {
    Client client = mock(Client.class);
    QueueFile queueFile = new QueueFile(new File(folder.getRoot(), "queue-file"));
    queueFile.add(TRACK_PAYLOAD_JSON.getBytes());
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    Client.Connection connection = mockConnection(urlConnection);
    when(client.upload()).thenReturn(connection);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().client(client).queueFile(queueFile).build();

    segmentDispatcher.performFlush();

    verify(urlConnection).disconnect();
  }

  @Test public void serializationErrorSkipsAddingPayload() throws IOException {
    QueueFile queueFile = mock(QueueFile.class);
    Cartographer cartographer = mock(Cartographer.class);
    BasePayload payload = mock(BasePayload.class);
    SegmentDispatcher segmentDispatcher =
        new SegmentBuilder().cartographer(cartographer).queueFile(queueFile).build();

    // Serialized json is null.
    when(cartographer.toJson(anyMap())).thenReturn(null);
    segmentDispatcher.performEnqueue(payload);
    verify(queueFile, never()).add((byte[]) any());

    // Serialized json is empty.
    when(cartographer.toJson(anyMap())).thenReturn("");
    segmentDispatcher.performEnqueue(payload);
    verify(queueFile, never()).add((byte[]) any());

    // Serialized json is too large (> 450kb).
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < 450001; i++) {
      stringBuilder.append('a');
    }
    when(cartographer.toJson(anyMap())).thenReturn(stringBuilder.toString());
    segmentDispatcher.performEnqueue(payload);
    verify(queueFile, never()).add((byte[]) any());

    // Serializing json throws exception.
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
    // Fill the payload with (1432 * 500) = ~716kb of data
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
