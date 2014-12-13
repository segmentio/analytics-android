package com.segment.analytics;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.TestUtils.mockApplication;
import static com.segment.analytics.Utils.toISO8601Date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class SegmentRobolectricTest {

  @Mock SegmentHTTPApi segmentHTTPApi;
  @Mock Stats stats;
  @Mock Logger logger;
  @Mock BasePayload payload;
  Context context;
  QueueFile queueFile;
  Segment segment;

  @Before public void setUp() {
    initMocks(this);
    context = mockApplication();
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
  }

  Segment createSegmentIntegration(int maxQueueSize) {
    return new Segment(context, maxQueueSize, 30, segmentHTTPApi, queueFile,
        Collections.<String, Boolean>emptyMap(), stats, logger);
  }

  QueueFile createQueueFile() throws IOException {
    File parent = Robolectric.getShadowApplication().getFilesDir();
    File queueFile = new File(parent, "wj8s1h5k-payload-v1");
    queueFile.delete();
    return new QueueFile(queueFile);
  }

  @Test public void addsToQueueCorrectly() throws IOException {
    queueFile = createQueueFile();
    segment = createSegmentIntegration(20);
    assertThat(queueFile.size()).isEqualTo(0);
    segment.performEnqueue(payload);
    assertThat(queueFile.size()).isEqualTo(1);
    segment.performEnqueue(payload);
    assertThat(queueFile.size()).isEqualTo(2);
  }

  @Test public void flushesQueueCorrectly() throws IOException {
    queueFile = createQueueFile();
    segment = createSegmentIntegration(20);
    segment.performEnqueue(payload);
    segment.performEnqueue(payload);
    segment.performEnqueue(payload);
    segment.performEnqueue(payload);

    segment.performFlush();
    try {
      verify(segmentHTTPApi).upload(any(SegmentHTTPApi.StreamWriter.class));
    } catch (IOException e) {
      fail("should not throw exception");
    }
    verify(stats).dispatchFlush(4);
    assertThat(queueFile.size()).isEqualTo(0);
  }

  @Test public void flushesWhenQueueHitsMax() throws IOException {
    queueFile = createQueueFile();
    segment = createSegmentIntegration(3);
    assertThat(queueFile.size()).isEqualTo(0);
    segment.performEnqueue(payload);
    segment.performEnqueue(payload);
    segment.performEnqueue(payload);

    try {
      verify(segmentHTTPApi).upload(any(SegmentHTTPApi.StreamWriter.class));
    } catch (IOException e) {
      fail("should not throw exception");
    }
    verify(stats).dispatchFlush(3);
    assertThat(queueFile.size()).isEqualTo(0);
  }

  @Test public void batchPayloadStreamWriter() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Segment.BatchPayloadStreamWriter batchPayloadStreamWriter =
        new Segment.BatchPayloadStreamWriter(byteArrayOutputStream);

    final HashMap<String, Boolean> integrations = new LinkedHashMap<String, Boolean>();
    integrations.put("foo", false);
    integrations.put("bar", true);

    batchPayloadStreamWriter.beginObject()
        .integrations(integrations)
        .beginBatchArray()
        .emitBatchItem("qaz")
        .emitBatchItem("qux")
        .endBatchArray()
        .endObject()
        .close();

    assertThat(byteArrayOutputStream.toString()).isEqualTo(
        "{\"integrations\":{\"foo\":false,\"bar\":true},\"batch\":[qaz,qux],\"sentAt\":\""
            + toISO8601Date(new Date())
            + "\"}").overridingErrorMessage("its ok if this failed close to midnight!");
  }
}
