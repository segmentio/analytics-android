package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.annotation.Config.NONE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class BatchPayloadWriterTest {

  @Test
  public void batchPayloadWriter() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentIntegration.BatchPayloadWriter batchPayloadWriter =
        new SegmentIntegration.BatchPayloadWriter(byteArrayOutputStream);

    batchPayloadWriter
        .beginObject()
        .beginBatchArray()
        .emitPayloadObject("foobarbazqux")
        .emitPayloadObject("{}")
        .emitPayloadObject("2")
        .endBatchArray()
        .endObject()
        .close();

    // todo: inject a fake clock. for now we'll compare a lower precision.
    assertThat(byteArrayOutputStream.toString()) //
        .contains("{\"batch\":[foobarbazqux,{},2],\"sentAt\":\"");
  }

  @Test
  public void batchPayloadWriterSingleItem() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentIntegration.BatchPayloadWriter batchPayloadWriter =
        new SegmentIntegration.BatchPayloadWriter(byteArrayOutputStream);

    batchPayloadWriter
        .beginObject()
        .beginBatchArray()
        .emitPayloadObject("qaz")
        .endBatchArray()
        .endObject()
        .close();

    // todo: inject a fake clock. for now we'll compare a lower precision.
    assertThat(byteArrayOutputStream.toString()) //
        .contains("{\"batch\":[qaz],\"sentAt\":\"");
  }

  @Test
  public void batchPayloadWriterFailsForNoItem() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentIntegration.BatchPayloadWriter batchPayloadWriter =
        new SegmentIntegration.BatchPayloadWriter(byteArrayOutputStream);

    try {
      batchPayloadWriter.beginObject().beginBatchArray().endBatchArray().endObject().close();
    } catch (IOException exception) {
      assertThat(exception).hasMessage("At least one payload must be provided.");
    }
  }
}
