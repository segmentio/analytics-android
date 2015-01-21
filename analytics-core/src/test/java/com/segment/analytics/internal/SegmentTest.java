package com.segment.analytics.internal;

import android.content.Context;
import com.segment.analytics.internal.model.payloads.BasePayload;
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
import static com.segment.analytics.internal.Utils.toISO8601Date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class SegmentTest {

  @Mock Client client;
  @Mock Stats stats;
  @Mock Logger logger;
  @Mock BasePayload payload;
  Context context;
  QueueFile queueFile;
  Segment segment;
  Cartographer cartographer;

  @Before public void setUp() {
    initMocks(this);
    context = mockApplication();
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    cartographer = Cartographer.INSTANCE;
  }

  Segment createSegmentIntegration(int maxQueueSize) {
    return new Segment(context, client, Cartographer.INSTANCE, queueFile, logger, stats,
        Collections.<String, Boolean>emptyMap(), 30, maxQueueSize);
  }

  QueueFile createQueueFile() throws IOException {
    File parent = Robolectric.getShadowApplication().getFilesDir();
    File queueFile = new File(parent, "wj8s1h5k-payload-v1");
    queueFile.delete();
    return new QueueFile(queueFile);
  }

  // todo: add tests that were removed in Client refactor

  @Test public void addsToQueueCorrectly() throws IOException {
    queueFile = createQueueFile();
    segment = createSegmentIntegration(20);
    assertThat(queueFile.size()).isEqualTo(0);
    segment.performEnqueue(payload);
    assertThat(queueFile.size()).isEqualTo(1);
    segment.performEnqueue(payload);
    assertThat(queueFile.size()).isEqualTo(2);
  }

  @Test public void batchPayloadWriter() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Segment.BatchPayloadWriter batchPayloadWriter =
        new Segment.BatchPayloadWriter(byteArrayOutputStream);

    final HashMap<String, Boolean> integrations = new LinkedHashMap<String, Boolean>();
    integrations.put("foo", false);
    integrations.put("bar", true);

    batchPayloadWriter.beginObject()
        .integrations(integrations)
        .beginBatchArray()
        .emitPayloadObject("qaz")
        .emitPayloadObject("qux")
        .emitPayloadObject("foobarbazqux")
        .endBatchArray()
        .endObject()
        .close();

    assertThat(byteArrayOutputStream.toString()).isEqualTo(
        "{\"integrations\":{\"foo\":false,\"bar\":true},\"batch\":[qaz,qux,foobarbazqux],"
            + "\"sentAt\":\""
            + toISO8601Date(new Date())
            + "\"}").overridingErrorMessage("its ok if this failed close to midnight!");
  }

  @Test public void batchPayloadWriterSingleItem() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Segment.BatchPayloadWriter batchPayloadWriter =
        new Segment.BatchPayloadWriter(byteArrayOutputStream);

    final HashMap<String, Boolean> integrations = new LinkedHashMap<String, Boolean>();
    integrations.put("foo", false);
    integrations.put("bar", true);

    batchPayloadWriter.beginObject()
        .integrations(integrations)
        .beginBatchArray()
        .emitPayloadObject("qaz")
        .endBatchArray()
        .endObject()
        .close();

    assertThat(byteArrayOutputStream.toString()).isEqualTo(
        "{\"integrations\":{\"foo\":false,\"bar\":true},\"batch\":[qaz],\"sentAt\":\""
            + toISO8601Date(new Date())
            + "\"}").overridingErrorMessage("its ok if this failed close to midnight!");
  }

  @Test public void batchPayloadWriterNoItem() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Segment.BatchPayloadWriter batchPayloadWriter =
        new Segment.BatchPayloadWriter(byteArrayOutputStream);

    final HashMap<String, Boolean> integrations = new LinkedHashMap<String, Boolean>();
    integrations.put("foo", false);
    integrations.put("bar", true);

    try {
      batchPayloadWriter.beginObject()
          .integrations(integrations)
          .beginBatchArray()
          .endBatchArray()
          .endObject()
          .close();
    } catch (IOException error) {
      assertThat(error).hasMessage("At least one payload must be provided.");
    }
  }
}
