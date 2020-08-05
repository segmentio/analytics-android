package com.segment.analytics

import com.segment.analytics.SegmentIntegration.BatchPayloadWriter
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.IOException


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BatchPayloadWriterTest {

  @Test
  @Throws(IOException::class)
  fun batchPayloadWriter() {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val batchPayloadWriter = BatchPayloadWriter(byteArrayOutputStream)
    batchPayloadWriter
        .beginObject()
        .beginBatchArray()
        .emitPayloadObject("foobarbazqux")
        .emitPayloadObject("{}")
        .emitPayloadObject("2")
        .endBatchArray()
        .endObject()
        .close()

    // todo: inject a fake clock. for now we'll compare a lower precision.
    Assertions.assertThat(byteArrayOutputStream.toString())
        .contains("{\"batch\":[foobarbazqux,{},2],\"sentAt\":\"")
  }

  @Test
  @Throws(IOException::class)
  fun batchPayloadWriterSingleItem() {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val batchPayloadWriter = BatchPayloadWriter(byteArrayOutputStream)
    batchPayloadWriter
        .beginObject()
        .beginBatchArray()
        .emitPayloadObject("qaz")
        .endBatchArray()
        .endObject()
        .close()

    // todo: inject a fake clock. for now we'll compare a lower precision.
    Assertions.assertThat(byteArrayOutputStream.toString())
        .contains("{\"batch\":[qaz],\"sentAt\":\"")
  }

  @Test
  @Throws(IOException::class)
  fun batchPayloadWriterFailsForNoItem() {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val batchPayloadWriter = BatchPayloadWriter(byteArrayOutputStream)

    try {
      batchPayloadWriter.beginObject().beginBatchArray().endBatchArray().endObject().close()
    } catch (exception: IOException) {
      Assertions.assertThat(exception).hasMessage("At least one payload must be provided.")
    }
  }

}