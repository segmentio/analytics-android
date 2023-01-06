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
package com.segment.analytics

import com.segment.analytics.SegmentIntegration.BatchPayloadWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
            .endObject("test")
            .close()

        // todo: inject a fake clock. for now we'll compare a lower precision.
        val json = byteArrayOutputStream.toString()
        assertThat(json)
            .contains("{\"batch\":[foobarbazqux,{},2],\"sentAt\":\"")
        assertThat(json).contains("\"writeKey\":\"test\"")
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
            .endObject("test")
            .close()

        // todo: inject a fake clock. for now we'll compare a lower precision.
        val json = byteArrayOutputStream.toString()
        assertThat(json)
            .contains("{\"batch\":[qaz],\"sentAt\":\"")
        assertThat(json).contains("\"writeKey\":\"test\"")
    }

    @Test
    @Throws(IOException::class)
    fun batchPayloadWriterFailsForNoItem() {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val batchPayloadWriter = BatchPayloadWriter(byteArrayOutputStream)

        try {
            batchPayloadWriter.beginObject().beginBatchArray().endBatchArray().endObject("test").close()
        } catch (exception: IOException) {
            assertThat(exception).hasMessage("At least one payload must be provided.")
        }
    }
}
