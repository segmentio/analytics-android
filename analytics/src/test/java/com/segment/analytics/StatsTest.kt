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

import android.util.Pair
import java.io.IOException
import org.assertj.core.api.Assertions
import org.assertj.core.data.MapEntry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StatsTest {
    private lateinit var stats: Stats

    @Before
    fun setUp() {
        stats = Stats()
    }

    @Test
    @Throws(IOException::class)
    fun performFlush() {
        stats.performFlush(4)
        Assertions.assertThat(stats.flushCount).isEqualTo(1)
        Assertions.assertThat(stats.flushEventCount).isEqualTo(4)

        stats.performFlush(10)
        Assertions.assertThat(stats.flushCount).isEqualTo(2)
        Assertions.assertThat(stats.flushEventCount).isEqualTo(14)
    }

    @Test
    @Throws(IOException::class)
    fun performIntegrationOperation() {
        stats.performIntegrationOperation(Pair("foo", 43L))
        Assertions.assertThat(stats.integrationOperationCount).isEqualTo(1)
        Assertions.assertThat(stats.integrationOperationDuration).isEqualTo(43L)
        Assertions.assertThat(stats.integrationOperationDurationByIntegration)
            .containsExactly(MapEntry.entry("foo", 43L))

        stats.performIntegrationOperation(Pair("bar", 2L))
        Assertions.assertThat(stats.integrationOperationCount).isEqualTo(2)
        Assertions.assertThat(stats.integrationOperationDuration).isEqualTo(45L)
        Assertions.assertThat(stats.integrationOperationDurationByIntegration)
            .hasSize(2)
            .contains(MapEntry.entry("bar", 2L))

        stats.performIntegrationOperation(Pair("bar", 19L))
        Assertions.assertThat(stats.integrationOperationCount).isEqualTo(3)
        Assertions.assertThat(stats.integrationOperationDuration).isEqualTo(64L)
        Assertions.assertThat(stats.integrationOperationDurationByIntegration)
            .hasSize(2)
            .contains(MapEntry.entry("bar", 21L))
    }

    @Test
    @Throws(IOException::class)
    fun createSnapshot() {
        stats.performFlush(1)
        stats.performFlush(1)
        stats.performFlush(2)
        stats.performFlush(3)
        stats.performFlush(5)
        stats.performFlush(8)
        stats.performFlush(13)
        stats.performFlush(21)

        stats.performIntegrationOperation(Pair("foo", 1L))
        stats.performIntegrationOperation(Pair("foo", 1L))
        stats.performIntegrationOperation(Pair("foo", 1L))
        stats.performIntegrationOperation(Pair("foo", 1L))
        stats.performIntegrationOperation(Pair("foo", 1L))
        stats.performIntegrationOperation(Pair("foo", 1L))

        stats.performIntegrationOperation(Pair("bar", 2L))
        stats.performIntegrationOperation(Pair("bar", 2L))
        stats.performIntegrationOperation(Pair("bar", 2L))
        stats.performIntegrationOperation(Pair("bar", 2L))

        val snapshot = stats.createSnapshot()
        Assertions.assertThat(snapshot.flushCount).isEqualTo(8)
        Assertions.assertThat(snapshot.flushEventCount).isEqualTo(54)
        Assertions.assertThat(snapshot.integrationOperationCount).isEqualTo(10)
        Assertions.assertThat(snapshot.integrationOperationDuration).isEqualTo(14L)
        Assertions.assertThat(snapshot.integrationOperationAverageDuration).isEqualTo(1.4f)
        Assertions.assertThat(snapshot.integrationOperationDurationByIntegration)
            .hasSize(2)
            .contains(MapEntry.entry("foo", 6L))
            .contains(MapEntry.entry("bar", 8L))

        try {
            snapshot.integrationOperationDurationByIntegration["qaz"] = 10L
            Assertions.fail("Map instance should be immutable.")
        } catch (ignored: UnsupportedOperationException) {
        }
    }

    @Test
    @Throws(IOException::class)
    fun createEmptySnapshot() {
        val snapshot: StatsSnapshot = stats.createSnapshot()

        Assertions.assertThat(snapshot.timestamp).isNotZero()
        Assertions.assertThat(snapshot.flushCount).isZero()
        Assertions.assertThat(snapshot.flushEventCount).isZero()
        Assertions.assertThat(snapshot.integrationOperationCount).isZero()
        Assertions.assertThat(snapshot.integrationOperationDuration).isZero()
        Assertions.assertThat(snapshot.integrationOperationAverageDuration).isZero()
        Assertions.assertThat(snapshot.integrationOperationDurationByIntegration).isEmpty()
    }
}
