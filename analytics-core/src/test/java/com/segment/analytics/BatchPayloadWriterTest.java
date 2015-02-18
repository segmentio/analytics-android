package com.segment.analytics;

import edu.emory.mathcs.backport.java.util.Collections;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.internal.Utils.toISO8601Date;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class BatchPayloadWriterTest {

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
}
