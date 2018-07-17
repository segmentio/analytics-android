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
