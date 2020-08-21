package com.segment.analytics

import com.squareup.burst.BurstJUnit4
import com.squareup.burst.annotation.Burst
import okio.ByteString
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

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
        Assertions.assertThat(queue.size()).isEqualTo(3)
    }

    @Test
    @Throws(IOException::class)
    fun forEach() {
        val seen = readQueue(queue.size() + 1)
        Assertions.assertThat(seen)
                .containsExactly(bytes("one"), bytes("two"), bytes("three"))
    }

    @Test
    @Throws(IOException::class)
    fun forEachEarlyReturn() {
        val seen = readQueue(2)
        Assertions.assertThat(seen).containsExactly(bytes("one"), bytes("two"))
    }

    @Test
    @Throws(IOException::class)
    fun remove() {
        queue.remove(2)
        Assertions.assertThat(queue.size()).isEqualTo(1)

        val seen = readQueue(1)
        Assertions.assertThat(seen).containsExactly(bytes("three"))
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
                    override fun read(`in`: InputStream, length: Int): Boolean {
                        val data = ByteArray(length)
                        Assertions.assertThat(`in`.read(data)).isEqualTo(length)
                        seen.add(data)
                        return count++ < maxCount
                    }
                })
        return seen
    }
}