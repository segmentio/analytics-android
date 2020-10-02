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

import com.squareup.burst.BurstJUnit4
import com.squareup.burst.annotation.Burst
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import kotlin.jvm.Throws
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class PayloadQueueTest {
    private enum class QueueFactory {
        FILE {
            @Throws(IOException::class)
            override fun create(queueFile: QueueFile?): PayloadQueue {
                return PayloadQueue.PersistentQueue(queueFile)
            }
        },
        MEMORY {
            override fun create(queueFile: QueueFile?): PayloadQueue {
                return PayloadQueue.MemoryQueue()
            }
        };

        @Throws(IOException::class)
        abstract fun create(queueFile: QueueFile?): PayloadQueue?
    }

    @Rule
    @JvmField var folder: TemporaryFolder = TemporaryFolder()

    @Burst
    private lateinit var factory: QueueFactory
    private lateinit var queue: PayloadQueue

    @Before
    @Throws(IOException::class)
    fun setUp() {
        val parent = folder.root
        val file = File(parent, "payload-queue")
        val queueFile = QueueFile(file)

        queue = factory.create(queueFile)!!
        queue.add(bytes("one"))
        queue.add(bytes("two"))
        queue.add(bytes("three"))
    }

    @Test
    @Throws(IOException::class)
    fun size() {
        assertThat(queue.size()).isEqualTo(3)
    }

    @Test
    @Throws(IOException::class)
    fun forEach() {
        val seen = readQueue(queue.size() + 1)
        assertThat(seen)
            .containsExactly(bytes("one"), bytes("two"), bytes("three"))
    }

    @Test
    @Throws(IOException::class)
    fun forEachEarlyReturn() {
        val seen = readQueue(2)
        assertThat(seen).containsExactly(bytes("one"), bytes("two"))
    }

    @Test
    @Throws(IOException::class)
    fun remove() {
        queue.remove(2)
        assertThat(queue.size()).isEqualTo(1)

        val seen = readQueue(1)
        assertThat(seen).containsExactly(bytes("three"))
    }

    private fun bytes(s: String): ByteArray {
        return ByteString.encodeUtf8(s).toByteArray()
    }

    @Throws(IOException::class)
    private fun readQueue(maxCount: Int): List<ByteArray> {
        val seen: MutableList<ByteArray> = ArrayList()
        queue.forEach(
            object : PayloadQueue.ElementVisitor {
                var count = 1

                @Throws(IOException::class)
                override fun read(input: InputStream, length: Int): Boolean {
                    val data = ByteArray(length)
                    assertThat(input.read(data)).isEqualTo(length)
                    seen.add(data)
                    return count++ < maxCount
                }
            })
        return seen
    }
}
