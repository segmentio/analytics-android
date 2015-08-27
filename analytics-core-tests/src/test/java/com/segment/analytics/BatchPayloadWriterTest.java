package com.segment.analytics;

import com.segment.analytics.core.tests.BuildConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.internal.Utils.toISO8601Date;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class BatchPayloadWriterTest {

  @Test public void batchPayloadWriter() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentDispatcher.BatchPayloadWriter batchPayloadWriter =
        new SegmentDispatcher.BatchPayloadWriter(byteArrayOutputStream);

    batchPayloadWriter.beginObject()
        .beginBatchArray()
        .emitPayloadObject("foobarbazqux")
        .emitPayloadObject("{}")
        .emitPayloadObject("2")
        .endBatchArray()
        .endObject()
        .close();

    assertThat(byteArrayOutputStream.toString()) //
        .isEqualTo("{"
            + "\"batch\":[foobarbazqux,{},2],"
            + "\"sentAt\":\""
            + toISO8601Date(new Date())
            + "\"}");
  }

  @Test public void batchPayloadWriterSingleItem() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentDispatcher.BatchPayloadWriter batchPayloadWriter =
        new SegmentDispatcher.BatchPayloadWriter(byteArrayOutputStream);

    batchPayloadWriter.beginObject()
        .beginBatchArray()
        .emitPayloadObject("qaz")
        .endBatchArray()
        .endObject()
        .close();

    assertThat(byteArrayOutputStream.toString()) //
        .isEqualTo("{\"batch\":[qaz],\"sentAt\":\"" + toISO8601Date(new Date()) + "\"}")
        .overridingErrorMessage("its ok if this failed close to midnight!");
  }

  @Test public void batchPayloadWriterFailsForNoItem() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    SegmentDispatcher.BatchPayloadWriter batchPayloadWriter =
        new SegmentDispatcher.BatchPayloadWriter(byteArrayOutputStream);

    try {
      batchPayloadWriter.beginObject().beginBatchArray().endBatchArray().endObject().close();
    } catch (IOException exception) {
      assertThat(exception).hasMessage("At least one payload must be provided.");
    }
  }
}
