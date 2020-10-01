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
package com.segment.analytics.integrations

import com.nhaarman.mockitokotlin2.any
import com.segment.analytics.integrations.TrackPayload.EVENT_KEY
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TrackPayloadTest {

    lateinit var builder: TrackPayload.Builder

    @Before
    fun setUp() {
        builder = TrackPayload.Builder().userId("userId")
    }

    @Test
    fun invalidEventThrows() {
        try {
            builder.event(any())
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("event cannot be null or empty")
        }

        try {
            builder.event("")
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("event cannot be null or empty")
        }
    }

    @Test
    fun event() {
        val payload = builder.event("event").build()
        assertThat(payload.event()).isEqualTo("event")
        assertThat(payload).containsEntry(EVENT_KEY, "event")
    }

    @Test
    fun eventIsRequired() {
        try {
            builder.build()
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("event cannot be null or empty")
        }
    }

    @Test
    fun invalidPropertiesThrow() {
        try {
            builder.properties(any())
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("properties == null")
        }
    }

    @Test
    fun properties() {
        val payload = builder
            .event("event")
            .properties(mapOf("foo" to "bar"))
            .build()
        assertThat(payload.properties())
            .isEqualTo(mapOf("foo" to "bar"))
        assertThat(payload)
            .containsEntry(TrackPayload.PROPERTIES_KEY, mapOf("foo" to "bar"))
    }
}
