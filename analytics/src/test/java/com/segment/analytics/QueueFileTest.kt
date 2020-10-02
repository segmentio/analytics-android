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

import com.segment.analytics.QueueFile.Element
import com.segment.analytics.QueueFile.HEADER_LENGTH
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import kotlin.NoSuchElementException
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QueueFileTest {
    private val logger = Logger.getLogger(QueueFileTest::class.java.name)

    /**
     * Takes up 33401 bytes in the queue (N*(N+1)/2+4*N). Picked 254 instead of 255 so that the number
     * of bytes isn't a multiple of 4.
     */
    private val N = 254
    private val values = arrayOfNulls<ByteArray>(N)

    private fun populate() {
        for (i in 0 until N) {
            val value = ByteArray(i)
            for (ii in 0 until N) {
                // Example: values[3] = { 3, 2, 1 }
                for (ii in 0 until i) value[ii] = (i - ii).toByte()
                values[i] = value
            }
        }
    }

    @Rule @JvmField val folder = TemporaryFolder()
    private lateinit var file: File

    @Before
    @Throws(Exception::class)
    fun setUp() {
        populate()
        val parent = folder.root
        file = File(parent, "queue-file")
    }

    @Test
    @Throws(IOException::class)
    fun testAddOneElement() {
        // This test ensures that we update 'first' correctly.
        var queue = QueueFile(file)
        val expected = values[253]
        queue.add(expected)
        assertThat(queue.peek()).isEqualTo(expected)
        queue.close()
        queue = QueueFile(file)
        assertThat(queue.peek()).isEqualTo(expected)
    }

    @Test
    @Throws(IOException::class)
    fun testClearErases() {
        val queue = QueueFile(file)
        val expected = values[253]
        queue.add(expected)

        // Confirm that the data was in the file before we cleared.
        val data = ByteArray(expected!!.size)
        queue.raf.seek((HEADER_LENGTH + Element.HEADER_LENGTH).toLong())
        queue.raf.readFully(data, 0, expected.size)
        assertThat(data).isEqualTo(expected)

        queue.clear()

        // Should have been erased.
        queue.raf.seek((HEADER_LENGTH + Element.HEADER_LENGTH).toLong())
        queue.raf.readFully(data, 0, expected.size)
        assertThat(data).isEqualTo(ByteArray(expected.size))
    }

    @Test
    @Throws(IOException::class)
    fun testClearDoesNotCorrupt() {
        var queue = QueueFile(file)
        val stuff = values[253]
        queue.add(stuff)
        queue.clear()

        queue = QueueFile(file)
        assertThat(queue.isEmpty).isTrue()
        assertThat(queue.peek()).isNull()

        queue.add(values[25])
        assertThat(queue.peek()).isEqualTo(values[25])
    }

    @Test
    @Throws(IOException::class)
    fun removeErasesEagerly() {
        val queue = QueueFile(file)

        val firstStuff = values[127]
        queue.add(firstStuff)

        val secondStuff = values[253]
        queue.add(secondStuff)

        // Confirm that first stuff was in the file before we remove.
        val data = ByteArray(firstStuff!!.size)
        queue.raf.seek((HEADER_LENGTH + Element.HEADER_LENGTH).toLong())
        queue.raf.readFully(data, 0, firstStuff.size)
        assertThat(data).isEqualTo(firstStuff)

        queue.remove()

        // Next record is intact
        assertThat(queue.peek()).isEqualTo(secondStuff)

        // First should have been erased.
        queue.raf.seek((HEADER_LENGTH + Element.HEADER_LENGTH).toLong())
        queue.raf.readFully(data, 0, firstStuff.size)
        assertThat(data).isEqualTo(ByteArray(firstStuff.size))
    }

    @Test
    @Throws(IOException::class)
    fun testZeroSizeInHeaderThrows() {
        val emptyFile = RandomAccessFile(file, "rwd")
        emptyFile.setLength(4096)
        emptyFile.channel.force(true)
        emptyFile.close()

        try {
            QueueFile(file)
            fail("Should have thrown about bad header length")
        } catch (e: IOException) {
            assertThat(e).hasMessage("File is corrupt; length stored in header (0) is invalid.")
        }
    }

    @Test
    @Throws(IOException::class)
    fun testNegativeSizeInHeaderThrows() {
        val emptyFile = RandomAccessFile(file, "rwd")
        emptyFile.seek(0)
        emptyFile.writeInt(-2147483648)
        emptyFile.setLength(4096)
        emptyFile.channel.force(true)
        emptyFile.close()

        try {
            QueueFile(file)
            fail("Should have thrown about bad header length")
        } catch (ex: IOException) {
            assertThat(ex)
                .hasMessage("File is corrupt; length stored in header (-2147483648) is invalid.")
        }
    }

    @Test
    @Throws(IOException::class)
    fun testInvalidFirstPositionThrows() {
        val emptyFile = RandomAccessFile(file, "rwd")
        emptyFile.seek(0)
        emptyFile.writeInt(4096)
        emptyFile.setLength(4096)
        emptyFile.seek(8)
        emptyFile.writeInt(10000)
        emptyFile.channel.force(true)
        emptyFile.close()

        try {
            QueueFile(file)
            fail("Should have thrown about bad first position value")
        } catch (ex: IOException) {
            assertThat(ex)
                .hasMessage("File is corrupt; first position stored in header (10000) is invalid.")
        }
    }

    @Test
    @Throws(IOException::class)
    fun testNegativeFirstPositionThrows() {
        val emptyFile = RandomAccessFile(file, "rwd")
        emptyFile.seek(0)
        emptyFile.writeInt(4096)
        emptyFile.setLength(4096)
        emptyFile.seek(8)
        emptyFile.writeInt(-2147483648)
        emptyFile.channel.force(true)
        emptyFile.close()

        try {
            QueueFile(file)
            fail("Should have thrown about first position value")
        } catch (ex: IOException) {
            assertThat(ex)
                .hasMessage("File is corrupt; first position stored in header (-2147483648) is invalid.")
        }
    }

    @Test
    @Throws(IOException::class)
    fun testInvalidLastPositionThrows() {
        val emptyFile = RandomAccessFile(file, "rwd")
        emptyFile.seek(0)
        emptyFile.writeInt(4096)
        emptyFile.setLength(4096)
        emptyFile.seek(12)
        emptyFile.writeInt(10000)
        emptyFile.channel.force(true)
        emptyFile.close()

        try {
            QueueFile(file)
            fail("Should have thrown about bad last position value")
        } catch (ex: IOException) {
            assertThat(ex)
                .hasMessage("File is corrupt; last position stored in header (10000) is invalid.")
        }
    }

    @Test
    @Throws(IOException::class)
    fun testNegativeLastPositionThrows() {
        val emptyFile = RandomAccessFile(file, "rwd")
        emptyFile.seek(0)
        emptyFile.writeInt(4096)
        emptyFile.setLength(4096)
        emptyFile.seek(12)
        emptyFile.writeInt(-2147483648)
        emptyFile.channel.force(true)
        emptyFile.close()

        try {
            QueueFile(file)
            Assert.fail("Should have thrown about bad last position value")
        } catch (ex: IOException) {
            assertThat(ex)
                .hasMessage("File is corrupt; last position stored in header (-2147483648) is invalid.")
        }
    }

    @Test
    @Throws(IOException::class)
    fun removeMultipleDoesNotCorrupt() {
        var queue = QueueFile(file)
        for (i in 0 until 10) {
            queue.add(values[i])
        }

        queue.remove(1)
        assertThat(queue.size()).isEqualTo(9)
        assertThat(queue.peek()).isEqualTo(values[1])

        queue.remove(3)
        queue = QueueFile(file)
        assertThat(queue.size()).isEqualTo(6)
        assertThat(queue.peek()).isEqualTo(values[4])

        queue.remove(6)
        assertThat(queue.isEmpty).isTrue()
        assertThat(queue.peek()).isNull()
    }

    @Test
    @Throws(IOException::class)
    fun removeDoesNotCorrupt() {
        var queue = QueueFile(file)
        queue.add(values[127])
        val secondStuff = values[253]
        queue.add(secondStuff)
        queue.remove()

        queue = QueueFile(file)
        assertThat(queue.peek()).isEqualTo(secondStuff)
    }

    @Test
    @Throws(IOException::class)
    fun removeFromEmptyFileThrows() {
        val queue = QueueFile(file)

        try {
            queue.remove()
            fail("Should have thrown about removing from empty file.")
        } catch (ignored: NoSuchElementException) {
        }
    }

    @Test
    @Throws(IOException::class)
    fun removeNegativeNumberOfElementsThrows() {
        val queue = QueueFile(file)
        queue.add(values[127])

        try {
            queue.remove(-1)
            Assert.fail("Should have thrown about removing negative number of elements.")
        } catch (ex: IllegalArgumentException) {
            assertThat(ex)
                .hasMessage("Cannot remove negative (-1) number of elements.")
        }
    }

    @Test
    @Throws(IOException::class)
    fun removeZeroElementsDoesNothing() {
        val queue = QueueFile(file)
        queue.add(values[127])

        queue.remove(0)
        assertThat(queue.size()).isEqualTo(1)
    }

    @Test
    @Throws(IOException::class)
    fun removeBeyondQueueSizeElementsThrows() {
        val queue = QueueFile(file)
        queue.add(values[127])

        try {
            queue.remove(10)
            fail("Should have thrown about removing too many elements.")
        } catch (ex: java.lang.IllegalArgumentException) {
            assertThat(ex)
                .hasMessage("Cannot remove more elements (10) than present in queue (1).")
        }
    }

    @Test
    @Throws(IOException::class)
    fun removingBigDamnBlocksErasesEffectively() {
        val bigBoy = ByteArray(7000)
        for (i in 0 until 7000 step 100) {
            values[100]!!.let {
                values[100]!!.size.let {
                    length ->
                    System.arraycopy(values[100]!!, 0, bigBoy, i, length)
                }
            }
        }

        val queue = QueueFile(file)
        queue.add(bigBoy)
        val secondStuff = values[123]
        queue.add(secondStuff)

        // Confirm that bigBoy was in the file before we remove.
        val data = ByteArray(bigBoy.size)
        queue.raf.seek((HEADER_LENGTH + Element.HEADER_LENGTH).toLong())
        queue.raf.readFully(data, 0, bigBoy.size)
        assertThat(data).isEqualTo(bigBoy)

        queue.remove()

        // Next record is intact
        assertThat(queue.peek()).isEqualTo(secondStuff)

        // First should have been erased.
        queue.raf.seek((HEADER_LENGTH + Element.HEADER_LENGTH).toLong())
        queue.raf.readFully(data, 0, bigBoy.size)
        assertThat(data).isEqualTo(ByteArray(bigBoy.size))
    }

    @Test
    @Throws(IOException::class)
    fun testAddAndRemoveElements() {
        val start = System.nanoTime()

        val expected: Queue<ByteArray> = LinkedList()
        for (round in 0 until 5) {
            val queue = QueueFile(file)
            for (i in 0 until N) {
                queue.add(values[i])
                expected.add(values[i])
            }
            // Leave N elements in round N, 15 total for 5 rounds. Removing all the
            // elements would be like starting with an empty queue.
            for (i in 0 until N - round - 1) {
                assertThat(queue.peek()).isEqualTo(expected.remove())
                queue.remove()
            }
            queue.close()
        }

        // Remove and validate remaining 15 elements.
        val queue = QueueFile(file)
        assertThat(queue.size()).isEqualTo(15)
        assertThat(queue.size()).isEqualTo(expected.size)
        while (!expected.isEmpty()) {
            assertThat(queue.peek()).isEqualTo(expected.remove())
            queue.remove()
        }
        queue.close()
        // length() returns 0, but I checked the size w/ 'ls', and it is correct.
        // assertEquals(65536, file.length());
        logger.info("Ran in " + (System.nanoTime() - start) / 1000000 + "ms.")
    }

    /** Tests queue expansion when the data crosses EOF. */
    @Test
    @Throws(IOException::class)
    fun testSplitExpansion() {
        // This should result in 3560 bytes.
        val max = 80

        val expected: Queue<ByteArray> = LinkedList()
        val queue = QueueFile(file)

        for (i in 0 until max) {
            expected.add(values[i])
            queue.add(values[i])
        }

        // Remove all but 1.
        for (i in 1 until max) {
            assertThat(queue.peek()).isEqualTo(expected.remove())
            queue.remove()
        }

        // This should wrap around before expanding.
        for (i in 0 until N) {
            expected.add(values[i])
            queue.add(values[i])
        }

        while (!expected.isEmpty()) {
            assertThat(queue.peek()).isEqualTo(expected.remove())
            queue.remove()
        }
        queue.close()
    }

    @Test
    @Throws(IOException::class)
    fun testFailedAdd() {
        var queueFile = QueueFile(file)
        queueFile.add(values[253])
        queueFile.close()

        val braf = BrokenRandomAccessFile(file, "rwd")
        queueFile = QueueFile(braf)

        try {
            queueFile.add(values[252])
            Assert.fail()
        } catch (e: IOException) {
            /* expected */
        }

        braf.rejectCommit = false

        // Allow a subsequent add to succeed.
        queueFile.add(values[251])

        queueFile.close()

        queueFile = QueueFile(file)
        assertThat(queueFile.size()).isEqualTo(2)
        assertThat(queueFile.peek()).isEqualTo(values[253])
        queueFile.remove()
        assertThat(queueFile.peek()).isEqualTo(values[251])
    }

    @Test
    @Throws(IOException::class)
    fun testFailedRemoval() {
        var queueFile = QueueFile(file)
        queueFile.add(values[253])
        queueFile.close()

        val braf = BrokenRandomAccessFile(file, "rwd")
        queueFile = QueueFile(braf)

        try {
            queueFile.remove()
            Assert.fail()
        } catch (e: IOException) {
            /* expected */
        }

        queueFile.close()

        queueFile = QueueFile(file)
        assertThat(queueFile.size()).isEqualTo(1)
        assertThat(queueFile.peek()).isEqualTo(values[253])

        queueFile.add(values[99])
        queueFile.remove()
        assertThat(queueFile.peek()).isEqualTo(values[99])
    }

    @Test
    @Throws(IOException::class)
    fun testFailedExpansion() {
        var queueFile = QueueFile(file)
        queueFile.add(values[253])
        queueFile.close()

        val braf = BrokenRandomAccessFile(file, "rwd")
        queueFile = QueueFile(braf)

        try {
            // This should trigger an expansion which should fail.
            queueFile.add(ByteArray(8000))
            Assert.fail()
        } catch (e: IOException) {
            /* expected */
        }

        queueFile.close()

        queueFile = QueueFile(file)

        assertThat(queueFile.size()).isEqualTo(1)
        assertThat(queueFile.peek()).isEqualTo(values[253])
        assertThat(queueFile.fileLength).isEqualTo(4096)

        queueFile.add(values[99])
        queueFile.remove()
        assertThat(queueFile.peek()).isEqualTo(values[99])
    }

    @Test
    @Throws(IOException::class)
    fun testForEachVisitor() {
        val queueFile = QueueFile(file)

        val a = byteArrayOf(1, 2)
        queueFile.add(a)
        val b = byteArrayOf(3, 4, 5)
        queueFile.add(b)

        val iteration = intArrayOf(0)
        val elementVisitor = PayloadQueue.ElementVisitor { input, length ->
            if (iteration[0] == 0) {
                assertThat(length).isEqualTo(2)
                val actual = ByteArray(length)
                input.read(actual)
                assertThat(actual).isEqualTo(a)
            } else if (iteration[0] == 1) {
                assertThat(length).isEqualTo(3)
                val actual = ByteArray(length)
                input.read(actual)
                assertThat(actual).isEqualTo(b)
            } else {
                Assert.fail()
            }
            iteration[0]++
            true
        }

        val saw = queueFile.forEach(elementVisitor)
        assertThat(saw).isEqualTo(2)
        assertThat(queueFile.peek()).isEqualTo(a)
        assertThat(iteration[0]).isEqualTo(2)
    }

    @Test
    @Throws(IOException::class)
    fun testForEachVisitorReadWithOffset() {
        val queueFile = QueueFile(file)

        queueFile.add(byteArrayOf(1, 2))
        queueFile.add(byteArrayOf(3, 4, 5))

        val actual = ByteArray(5)
        val offset = intArrayOf(0)

        val elementVisitor = PayloadQueue.ElementVisitor { input, length ->
            input.read(actual, offset[0], length)
            offset[0] += length
            true
        }

        val saw = queueFile.forEach(elementVisitor)
        assertThat(saw).isEqualTo(2)
        assertThat(actual).isEqualTo(byteArrayOf(1, 2, 3, 4, 5))
    }

    @Test
    @Throws(IOException::class)
    fun testForEachVisitorStreamCopy() {
        val queueFile = QueueFile(file)
        queueFile.add(byteArrayOf(1, 2))
        queueFile.add(byteArrayOf(3, 4, 5))

        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(8)

        val elementVisitor = PayloadQueue.ElementVisitor { input, length -> // A common idiom for copying data between two streams, but it depends on the
            // InputStream correctly returning -1 when no more data is available
            var count: Int
            while (input.read(buffer).also { count = it } != -1) {
                if (count == 0) {
                    // In the past, the ElementInputStream.read(byte[], int, int) method would return 0
                    // when no more bytes were available for reading. This test detects that error.
                    //
                    // Note: 0 is a valid return value for InputStream.read(byte[], int, int), which
                    // happens
                    // when the passed length is zero. We could trigger that through
                    // InputStream.read(byte[])
                    // by passing a zero-length buffer. However, since we won't do that during this
                    // test,
                    // we can safely assume that a return value of 0 indicates the past error in logic.
                    Assert.fail("This test should never receive a result of 0 from InputStream.read(byte[])")
                }
                baos.write(buffer, 0, count)
            }
            true
        }

        val saw = queueFile.forEach(elementVisitor)
        assertThat(saw).isEqualTo(2)
        assertThat(baos.toByteArray()).isEqualTo(byteArrayOf(1, 2, 3, 4, 5))
    }

    @Test
    @Throws(IOException::class)
    fun testForEachCanAbortEarly() {
        val queueFile = QueueFile(file)
        val a = byteArrayOf(1, 2)
        queueFile.add(a)
        val b = byteArrayOf(3, 4, 5)
        queueFile.add(b)
        val iteration = AtomicInteger()
        val elementVisitor = PayloadQueue.ElementVisitor { input, length ->
            if (iteration.get() == 0) {
                assertThat(length).isEqualTo(2)
                val actual = ByteArray(length)
                input.read(actual)
                assertThat(actual).isEqualTo(a)
            } else {
                Assert.fail()
            }
            iteration.incrementAndGet()
            false
        }
        val saw = queueFile.forEach(elementVisitor)
        assertThat(saw).isEqualTo(1)
        assertThat(queueFile.peek()).isEqualTo(a)
        assertThat(iteration.get()).isEqualTo(1)
    }

    /**
     * Exercise a bug where wrapped elements were getting corrupted when the QueueFile was forced to
     * expand in size and a portion of the final Element had been wrapped into space at the beginning
     * of the file.
     */

    @Test
    @Throws(IOException::class)
    fun testFileExpansionDoesntCorruptWrappedElements() {
        val queue = QueueFile(file)
        // Create test data - 1k blocks marked consecutively 1, 2, 3, 4 and 5.
        val values = arrayOfNulls<ByteArray>(5)
        for (blockNum in values.indices) {
            values[blockNum] = ByteArray(1024)
            for (i in 0 until (values[blockNum]!!.size)) {
                values[blockNum]!![i] = (blockNum + 1).toByte()
            }
        }

        // First, add the first two blocks to the queue, remove one leaving a
        // 1K space at the beginning of the buffer.
        queue.add(values[0])
        queue.add(values[1])
        queue.remove()

        // The trailing end of block "4" will be wrapped to the start of the buffer.
        queue.add(values[2])
        queue.add(values[3])

        // Cause buffer to expand as there isn't space between the end of block "4"
        // and the start of block "2".  Internally the queue should cause block "4"
        // to be contiguous, but there was a bug where that wasn't happening.
        queue.add(values[4])

        // Make sure values are not corrupted, specifically block "4" that wasn't
        // being made contiguous in the version with the bug.
        for (blockNum in 1 until values.size) {
            val value = queue.peek()
            queue.remove()

            for (i in value.indices) {
                assertThat(value[i])
                    .isEqualTo((blockNum + 1).toByte()) to "Block " + (blockNum + 1) + " corrupted at byte index " + i
            }
        }

        queue.close()
    }

    /**
     * Exercise a bug where wrapped elements were getting corrupted when the QueueFile was forced to
     * expand in size and a portion of the final Element had been wrapped into space at the beginning
     * of the file - if multiple Elements have been written to empty buffer space at the start does
     * the expansion correctly update all their positions?
     */
    @Test
    @Throws(IOException::class)
    fun testFileExpansionCorrectlyMovesElements() {
        val queue = QueueFile(file)

        // Create test data - 1k blocks marked consecutively 1, 2, 3, 4 and 5.
        val values = arrayOfNulls<ByteArray>(5)
        for (blockNum in values.indices) {
            values[blockNum] = ByteArray(1024)
            for (i in 0 until (values[blockNum]!!.size)) {
                values[blockNum]!![i] = (blockNum + 1).toByte()
            }
        }

        // smaller data elements
        val smaller = arrayOfNulls<ByteArray>(3)
        for (blockNum in smaller.indices) {
            smaller[blockNum] = ByteArray(256)
            for (i in smaller[blockNum]!!.indices) {
                smaller[blockNum]!![i] = (blockNum + 6).toByte()
            }
        }

        // First, add the first two blocks to the queue, remove one leaving a
        // 1K space at the beginning of the buffer.
        queue.add(values[0])
        queue.add(values[1])
        queue.remove()
        // The trailing end of block "4" will be wrapped to the start of the buffer.
        queue.add(values[2])
        queue.add(values[3])

        // Now fill in some space with smaller blocks, none of which will cause
        // an expansion.
        queue.add(smaller[0])
        queue.add(smaller[1])
        queue.add(smaller[2])

        // Cause buffer to expand as there isn't space between the end of the
        // smaller block "8" and the start of block "2".  Internally the queue
        // should cause all of tbe smaller blocks, and the trailing end of
        // block "5" to be moved to the end of the file.
        queue.add(values[4])

        val expectedBlockNumbers = byteArrayOf(2, 3, 4, 6, 7, 8, 5)

        // Make sure values are not corrupted, specifically block "4" that wasn't
        // being made contiguous in the version with the bug.
        for (expectedBlockNumber in expectedBlockNumbers) {
            val value = queue.peek()
            queue.remove()

            for (i in value.indices) {
                assertThat(value[i])
                    .isEqualTo(expectedBlockNumber) to "Block $expectedBlockNumber corrupted at byte index $i"
            }
        }

        queue.close()
    }

    /**
     * Exercise a bug where file expansion would leave garbage at the start of the header and after
     * the last element.
     */
    @Test
    @Throws(IOException::class)
    fun testFileExpansionCorrectlyZeroesData() {
        val queue = QueueFile(file)

        // Create test data - 1k blocks marked consecutively 1, 2, 3, 4 and 5.

        // Create test data - 1k blocks marked consecutively 1, 2, 3, 4 and 5.
        val values = arrayOfNulls<ByteArray>(5)
        for (blockNum in values.indices) {
            values[blockNum] = ByteArray(1024)
            for (i in values[blockNum]!!.indices) {
                values[blockNum]!![i] = (blockNum + 1).toByte()
            }
        }

        // First, add the first two blocks to the queue, remove one leaving a
        // 1K space at the beginning of the buffer.
        queue.add(values[0])
        queue.add(values[1])
        queue.remove()

        // The trailing end of block "4" will be wrapped to the start of the buffer.
        queue.add(values[2])
        queue.add(values[3])

        // Cause buffer to expand as there isn't space between the end of block "4"
        // and the start of block "2". Internally the queue will cause block "4"
        // to be contiguous. There was a bug where the start of the buffer still
        // contained the tail end of block "4", and garbage was copied after the tail
        // end of the last element.
        queue.add(values[4])

        // Read from header to first element and make sure it's zeroed.
        val firstElementPadding = 1028
        var data = ByteArray(firstElementPadding)
        queue.raf.seek(HEADER_LENGTH.toLong())
        queue.raf.readFully(data, 0, firstElementPadding)
        assertThat(data).containsOnly(0x00.toByte())

        // Read from the last element to the end and make sure it's zeroed.
        val endOfLastElement = HEADER_LENGTH + firstElementPadding + 4 * (Element.HEADER_LENGTH + 1024)
        val readLength = (queue.raf.length() - endOfLastElement).toInt()
        data = ByteArray(readLength)
        queue.raf.seek(endOfLastElement.toLong())
        queue.raf.readFully(data, 0, readLength)
        assertThat(data).containsOnly(0x00.toByte())
    }

    /**
     * Exercise a bug where an expanding queue file where the start and end positions are the same
     * causes corruption.
     */
    @Test
    @Throws(IOException::class)
    fun testSaturatedFileExpansionMovesElements() {
        val queue = QueueFile(file)
        // Create test data - 1016-byte blocks marked consecutively 1, 2, 3, 4, 5 and 6,
        // four of which perfectly fill the queue file, taking into account the file header
        // and the item headers.
        // Each item is of length
        // (QueueFile.INITIAL_LENGTH - QueueFile.HEADER_LENGTH) / 4 - element_header_length
        //    // = 1016 bytes
        val values = arrayOfNulls<ByteArray>(6)
        for (blockNum in values.indices) {
            values[blockNum] = ByteArray(1016)
            for (i in values[blockNum]!!.indices) {
                values[blockNum]!![i] = (blockNum + 1).toByte()
            }
        }

        // Saturate the queue file
        queue.add(values[0])
        queue.add(values[1])
        queue.add(values[2])
        queue.add(values[3])

        // Remove an element and add a new one so that the position of the start and
        // end of the queue are equal
        queue.remove()
        queue.add(values[4])

        // Cause the queue file to expand
        queue.add(values[5])

        // Make sure values are not corrupted
        for (i in 1..5) {
            assertThat(queue.peek()).isEqualTo(values[i])
            queue.remove()
        }

        queue.close()
    }

    /**
     * Exercise a bug where opening a queue whose first or last element's header was non contiguous
     * throws an {@link java.io.EOFException}.
     */
    @Test
    @Throws(IOException::class)
    fun testReadHeadersFromNonContiguousQueueWorks() {
        val queueFile = QueueFile(file)

        // Fill the queue up to `length - 2` (i.e. remainingBytes() == 2).
        for (i in 0 until 15) {
            queueFile.add(values[N - 1])
        }
        queueFile.add(values[219])

        // Remove first item so we have room to add another one without growing the file.
        queueFile.remove()

        // Add any element element and close the queue.
        queueFile.add(values[6])
        val queueSize = queueFile.size()
        queueFile.close()

        // File should not be corrupted.
        val queueFile2 = QueueFile(file)
        assertThat(queueFile2.size()).isEqualTo(queueFile.size())
    }

    /*
  @Test public void testOverflow() throws IOException {
    QueueFile queueFile = new QueueFile(file);

    // Create a 32k block of test data.
    byte[] value = new byte[32768];

    // Run 32764 iterations = (32768 + 4) * 32764 = 1073741808 bytes.
    for (int i = 0; i < 32764; i++) {
      queueFile.add(value);
    }

    // Grow the file to 1073741808 + (32768 + 4) = 1073774580 bytes.
    try {
      queueFile.add(value);
      fail("QueueFile should throw error if attempted to grow beyond 1073741824 bytes");
    } catch (EOFException e) {
      assertThat(e).hasMessage("Cannot grow file beyond 1073741824 bytes");
    }
  }
  */
    /** A RandomAccessFile that can break when you go to write the COMMITTED status.  */
    internal class BrokenRandomAccessFile(file: File, mode: String) : RandomAccessFile(file, mode) {
        var rejectCommit = true

        @Throws(IOException::class)
        override fun write(buffer: ByteArray) {
            if (rejectCommit && filePointer == 0L) {
                throw IOException("No commit for you!")
            }
            super.write(buffer)
        }
    }
}
