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

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.net.ConnectivityManager
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.segment.analytics.PayloadQueue.PersistentQueue
import com.segment.analytics.SegmentIntegration.BatchPayloadWriter
import com.segment.analytics.SegmentIntegration.MAX_PAYLOAD_SIZE
import com.segment.analytics.SegmentIntegration.MAX_QUEUE_SIZE
import com.segment.analytics.SegmentIntegration.PayloadWriter
import com.segment.analytics.SegmentIntegration.UTF_8
import com.segment.analytics.TestUtils.SynchronousExecutor
import com.segment.analytics.TestUtils.TRACK_PAYLOAD
import com.segment.analytics.TestUtils.TRACK_PAYLOAD_JSON
import com.segment.analytics.TestUtils.mockApplication
import com.segment.analytics.integrations.Logger
import com.segment.analytics.integrations.Logger.with
import com.segment.analytics.integrations.TrackPayload.Builder
import com.segment.analytics.internal.Utils.DEFAULT_FLUSH_INTERVAL
import com.segment.analytics.internal.Utils.DEFAULT_FLUSH_QUEUE_SIZE
import com.segment.analytics.internal.Utils.parseISO8601Date
import java.io.File
import java.io.IOError
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.util.concurrent.ExecutorService
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor.forClass
import org.mockito.Matchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SegmentIntegrationTest {

    companion object {
        const val DEFAULT_API_HOST = "api.segment.io/v1"
    }

    @Rule @JvmField
    val folder = TemporaryFolder()
    private lateinit var queueFile: QueueFile

    private fun mockConnection(): Client.Connection {
        return mockConnection(mock(HttpURLConnection::class.java))
    }

    private fun mockConnection(connection: HttpURLConnection): Client.Connection {
        return object : Client.Connection(
            connection,
            mock(InputStream::class.java),
            mock(OutputStream::class.java)
        ) {
            @Throws(IOException::class)
            override fun close() {
                super.close()
            }
        }
    }

    @Before
    @Throws(IOException::class)
    fun setUp() {
        queueFile = QueueFile(File(folder.root, "queue-file"))
    }

    @After
    fun tearDown() {
        assertThat(ShadowLog.getLogs()).isEmpty()
    }

    @Test
    @Throws(IOException::class)
    fun enqueueAddsToQueueFile() {
        val payloadQueue = PersistentQueue(queueFile)
        val segmentIntegration = SegmentBuilder().payloadQueue(payloadQueue).build()
        segmentIntegration.performEnqueue(TRACK_PAYLOAD)
        assertThat(payloadQueue.size()).isEqualTo(1)
    }

    @Test
    @Throws(IOException::class)
    fun enqueueWritesIntegrations() {
        val integrations = LinkedHashMap<String, Boolean>()
        integrations["All"] = false // should overwrite existing values in the map.
        integrations["Segment.io"] = false // should ignore Segment setting in payload.
        integrations["foo"] = true // should add new values.
        val payloadQueue = mock(PayloadQueue::class.java)
        val segmentIntegration =
            SegmentBuilder()
                .payloadQueue(payloadQueue)
                .integrations(integrations)
                .build()

        val trackPayload =
            Builder()
                .messageId("a161304c-498c-4830-9291-fcfb8498877b")
                .timestamp(parseISO8601Date("2014-12-15T13:32:44-0700"))
                .event("foo")
                .userId("userId")
                .build()

        segmentIntegration.performEnqueue(trackPayload)
        val expected =
            (
                """
                      {
                        "channel": "mobile",
                        "type": "track",
                        "messageId": "a161304c-498c-4830-9291-fcfb8498877b",
                        "timestamp": "2014-12-15T20:32:44.000Z",
                        "context": {},
                        "integrations": {
                            "All": false,
                            "foo": true
                        },
                        "userId": "userId",                                                                                                            
                        "anonymousId": null,
                        "event": "foo",
                        "properties": {}
                      }  
                """.trimIndent().replace("\n", "").replace(" ", "")
                )
        val captor = forClass(ByteArray::class.java)
        verify(payloadQueue).add(captor.capture())
        val got = String(captor.value, UTF_8)
        assertThat(got).isEqualTo(expected)
    }

    @Test
    @Throws(IOException::class)
    fun enqueueLimitsQueueSize() {
        val payloadQueue = mock(PayloadQueue::class.java)
        // We want to trigger a remove, but not a flush.
        whenever(payloadQueue.size()).thenReturn(0, MAX_QUEUE_SIZE, MAX_QUEUE_SIZE, 0)
        val segmentIntegration = SegmentBuilder().payloadQueue(payloadQueue).build()

        segmentIntegration.performEnqueue(TRACK_PAYLOAD)

        verify(payloadQueue).remove(1) // Oldest entry is removed.
        verify(payloadQueue).add(any(ByteArray::class.java)) // Newest entry is added.
    }

    @Test
    @Throws(IOException::class)
    fun exceptionIgnoredIfFailedToRemove() {
        val payloadQueue = mock(PayloadQueue::class.java)
        doThrow(IOException("no remove for you.")).whenever(payloadQueue).remove(1)
        whenever(payloadQueue.size()).thenReturn(MAX_QUEUE_SIZE) // trigger a remove
        val segmentIntegration = SegmentBuilder().payloadQueue(payloadQueue).build()

        try {
            segmentIntegration.performEnqueue(TRACK_PAYLOAD)
        } catch (unexpected: IOError) {
            fail("did not expect QueueFile to throw an error.")
        }

        verify(payloadQueue, never()).add(any(ByteArray::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun enqueueMaxTriggersFlush() {
        val payloadQueue = PersistentQueue(queueFile)
        val client = mock(Client::class.java)
        val connection = mockConnection()
        whenever(client.upload(DEFAULT_API_HOST)).thenReturn(connection)
        val segmentIntegration =
            SegmentBuilder()
                .client(client)
                .flushSize(5)
                .payloadQueue(payloadQueue)
                .build()
        for (i in 0 until 4) {
            segmentIntegration.performEnqueue(TRACK_PAYLOAD)
        }
        verifyZeroInteractions(client)
        // Only the last enqueue should trigger an upload.
        segmentIntegration.performEnqueue(TRACK_PAYLOAD)

        verify(client).upload(DEFAULT_API_HOST)
    }

    @Test
    @Throws(IOException::class)
    fun flushRemovesItemsFromQueue() {
        val payloadQueue = PersistentQueue(queueFile)
        val client = mock(Client::class.java)
        whenever(client.upload(DEFAULT_API_HOST)).thenReturn(mockConnection())
        val segmentIntegration =
            SegmentBuilder()
                .client(client)
                .payloadQueue(payloadQueue)
                .build()
        val bytes = TRACK_PAYLOAD_JSON.toByteArray()
        for (i in 0 until 4) {
            queueFile.add(bytes)
        }

        segmentIntegration.submitFlush()

        assertThat(queueFile.size()).isEqualTo(0)
    }

    @Test
    @Throws(IOException::class)
    fun flushSubmitsToExecutor() {
        val executor = spy(SynchronousExecutor())
        val payloadQueue = mock(PayloadQueue::class.java)
        whenever(payloadQueue.size()).thenReturn(1)
        val dispatcher =
            SegmentBuilder()
                .payloadQueue(payloadQueue)
                .networkExecutor(executor)
                .build()

        dispatcher.submitFlush()

        verify(executor).submit(any(Runnable::class.java))
    }

    @Test
    fun flushChecksIfExecutorIsShutdownFirst() {
        val executor = spy(SynchronousExecutor())
        val payloadQueue = mock(PayloadQueue::class.java)
        whenever(payloadQueue.size()).thenReturn(1)
        val dispatcher =
            SegmentBuilder()
                .payloadQueue(payloadQueue)
                .networkExecutor(executor)
                .build()

        dispatcher.shutdown()
        executor.shutdown()
        dispatcher.submitFlush()

        verify(executor, never()).submit(any(Runnable::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun flushWhenDisconnectedSkipsUpload() {
        val networkInfo = mock(android.net.NetworkInfo::class.java)
        whenever(networkInfo.isConnectedOrConnecting).thenReturn(false)
        val connectivityManager = mock(ConnectivityManager::class.java)
        whenever(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)
        val context: Context = mockApplication()
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        val client = mock(Client::class.java)
        val segmentIntegration = SegmentBuilder().context(context).client(client).build()

        segmentIntegration.submitFlush()

        verify(client, never()).upload(DEFAULT_API_HOST)
    }

    @Test
    @Throws(IOException::class)
    fun flushWhenQueueSizeIsLessThanOneSkipsUpload() {
        val payloadQueue = mock(PayloadQueue::class.java)
        whenever(payloadQueue.size()).thenReturn(0)
        val context: Context = mockApplication()
        val client = mock(Client::class.java)
        val segmentIntegration = SegmentBuilder()
            .payloadQueue(payloadQueue)
            .context(context)
            .client(client)
            .build()

        segmentIntegration.submitFlush()

        verifyZeroInteractions(context)
        verify(client, never()).upload(DEFAULT_API_HOST)
    }

    @Test
    @Throws(IOException::class)
    fun flushDisconnectsConnection() {
        val client = mock(Client::class.java)
        val payloadQueue = PersistentQueue(queueFile)
        queueFile.add(TRACK_PAYLOAD_JSON.toByteArray())
        val urlConnection = mock(HttpURLConnection::class.java)
        val connection = mockConnection(urlConnection)
        whenever(client.upload(DEFAULT_API_HOST)).thenReturn(connection)
        val segmentIntegration =
            SegmentBuilder()
                .client(client)
                .payloadQueue(payloadQueue)
                .build()

        segmentIntegration.submitFlush()

        verify(urlConnection, times(2)).disconnect()
    }

    @Test
    @Throws(IOException::class)
    fun removesRejectedPayloads() {
        // todo: rewrite using mockwebserver.
        val client = mock(Client::class.java)
        val payloadQueue = PersistentQueue(queueFile)
        whenever(client.upload(DEFAULT_API_HOST))
            .thenReturn(
                object : Client.Connection(
                    mock(HttpURLConnection::class.java), mock(InputStream::class.java), mock(OutputStream::class.java)
                ) {
                    @Throws(IOException::class)
                    override fun close() {
                        throw Client.HTTPException(400, "Bad Request", "bad request")
                    }
                })
        val segmentIntegration =
            SegmentBuilder()
                .client(client)
                .payloadQueue(payloadQueue)
                .build()
        for (i in 0..3) {
            payloadQueue.add(TRACK_PAYLOAD_JSON.toByteArray())
        }

        segmentIntegration.submitFlush()

        assertThat(queueFile.size()).isEqualTo(0)
        verify(client).upload(DEFAULT_API_HOST)
    }

    @Test
    @Throws(IOException::class)
    fun ignoresServerError() {
        // todo: rewrite using mockwebserver.
        val payloadQueue: PayloadQueue = PersistentQueue(queueFile)
        val client = mock(Client::class.java)
        whenever(client.upload(DEFAULT_API_HOST))
            .thenReturn(
                object : Client.Connection(
                    mock(HttpURLConnection::class.java), mock(InputStream::class.java), mock(OutputStream::class.java)
                ) {
                    @Throws(IOException::class)
                    override fun close() {
                        throw Client.HTTPException(
                            500, "Internal Server Error", "internal server error"
                        )
                    }
                })
        val segmentIntegration = SegmentBuilder()
            .client(client)
            .payloadQueue(payloadQueue)
            .build()
        for (i in 0..3) {
            payloadQueue.add(TRACK_PAYLOAD_JSON.toByteArray())
        }
        segmentIntegration.submitFlush()
        assertThat(queueFile.size()).isEqualTo(4)
        verify(client).upload(DEFAULT_API_HOST)
    }

    @Test
    @Throws(IOException::class)
    fun ignoresHTTP429Error() {
        // todo: rewrite using mockwebserver.
        val payloadQueue: PayloadQueue = PersistentQueue(queueFile)
        val client = mock(Client::class.java)
        whenever(client.upload(DEFAULT_API_HOST))
            .thenReturn(
                object : Client.Connection(
                    mock(
                        HttpURLConnection::class.java
                    ),
                    mock(InputStream::class.java),
                    mock(OutputStream::class.java)
                ) {
                    @Throws(IOException::class)
                    override fun close() {
                        throw Client.HTTPException(429, "Too Many Requests", "too many requests")
                    }
                })
        val segmentIntegration = SegmentBuilder()
            .client(client)
            .payloadQueue(payloadQueue)
            .build()
        for (i in 0..3) {
            payloadQueue.add(TRACK_PAYLOAD_JSON.toByteArray())
        }
        segmentIntegration.submitFlush()

        // Verify that messages were not removed from the queue when server returned a 429.
        assertThat(queueFile.size()).isEqualTo(4)
        verify(client).upload(DEFAULT_API_HOST)
    }

    @Test
    @Throws(IOException::class)
    fun serializationErrorSkipsAddingPayload() {
        val payloadQueue = mock(PayloadQueue::class.java)
        val cartographer = mock(Cartographer::class.java)
        val payload = Builder().event("event").userId("userId").build()
        val segmentIntegration = SegmentBuilder()
            .cartographer(cartographer)
            .payloadQueue(payloadQueue)
            .build()

        // Serialized json is null.
        whenever(cartographer.toJson(any(Map::class.java))).thenReturn(null)
        segmentIntegration.performEnqueue(payload)
        verify(payloadQueue, never()).add(any<Any>() as ByteArray?)

        // Serialized json is empty.
        whenever(cartographer.toJson(any(Map::class.java))).thenReturn("")
        segmentIntegration.performEnqueue(payload)
        verify(payloadQueue, never()).add(any<Any>() as ByteArray?)

        // Serialized json is too large (> MAX_PAYLOAD_SIZE).
        val stringBuilder = StringBuilder()
        for (i in 0..MAX_PAYLOAD_SIZE) {
            stringBuilder.append("a")
        }
        whenever(cartographer.toJson(any(Map::class.java))).thenReturn(stringBuilder.toString())
        segmentIntegration.performEnqueue(payload)
        verify(payloadQueue, never()).add(any<Any>() as ByteArray?)
    }

    @Test
    @Throws(IOException::class)
    fun shutDown() {
        val payloadQueue = mock(PayloadQueue::class.java)
        val segmentIntegration = SegmentBuilder().payloadQueue(payloadQueue).build()

        segmentIntegration.shutdown()

        verify(payloadQueue).close()
    }

    @Test
    @Throws(IOException::class)
    fun payloadVisitorReadsOnly475KB() {
        val payloadWriter = PayloadWriter(
            mock(BatchPayloadWriter::class.java), Crypto.none()
        )
        val bytes =
            """{
        "context": {
          "library": "analytics-android",
          "libraryVersion": "0.4.4",
          "telephony": {
            "radio": "gsm",
            "carrier": "FI elisa"
          },
          "wifi": {
            "connected": false,
            "available": false
          },
          "providers": {
            "Tapstream": false,
            "Amplitude": false,
            "Localytics": false,
            "Flurry": false,
            "Countly": false,
            "Bugsnag": false,
            "Quantcast": false,
            "Crittercism": false,
            "Google Analytics": false,
            "Omniture": false,
            "Mixpanel": false
          },
          "location": {
            "speed": 0,
            "longitude": 24.937207,
            "latitude": 60.2495497
          },
          "locale": {
            "carrier": "FI elisa",
            "language": "English",
            "country": "United States"
          },
          "device": {
            "userId": "123",
            "brand": "samsung",
            "release": "4.2.2",
            "manufacturer": "samsung",
            "sdk": 17
          },
          "display": {
            "density": 1.5,
            "width": 800,
            "height": 480
          },
          "build": {
            "name": "1.0",
            "code": 1
          },
          "ip": "80.186.195.102",
          "inferredIp": true
        }
      }""".toByteArray() // length 1432
        // Fill the payload with (1432 * 500) = ~716kb of data

        for (i in 0..499) {
            queueFile.add(bytes)
        }

        queueFile.forEach(payloadWriter)

        // Verify only (331 * 1432) = 473992 < 475KB bytes are read
        assertThat(payloadWriter.payloadCount).isEqualTo(331)
    }

    internal class SegmentBuilder() {
        var client: Client? = null
        var stats: Stats? = null
        var payloadQueue: PayloadQueue? = null
        var context: Context? = null
        var cartographer: Cartographer? = null
        var integrations: Map<String, Boolean>? = null
        var flushInterval = DEFAULT_FLUSH_INTERVAL
        var flushSize = DEFAULT_FLUSH_QUEUE_SIZE
        var logger = with(Analytics.LogLevel.NONE)
        var networkExecutor: ExecutorService? = null

        fun SegmentBuilder() {
            initMocks(this)
            context = mockApplication()
            whenever(context!!.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)) //
                .thenReturn(PERMISSION_DENIED)
            cartographer = Cartographer.INSTANCE
        }

        fun client(client: Client): SegmentBuilder {
            this.client = client
            return this
        }

        fun stats(stats: Stats): SegmentBuilder {
            this.stats = stats
            return this
        }

        fun payloadQueue(payloadQueue: PayloadQueue): SegmentBuilder {
            this.payloadQueue = payloadQueue
            return this
        }

        fun context(context: Context): SegmentBuilder {
            this.context = context
            return this
        }

        fun cartographer(cartographer: Cartographer): SegmentBuilder {
            this.cartographer = cartographer
            return this
        }

        fun integrations(integrations: Map<String, Boolean>): SegmentBuilder {
            this.integrations = integrations
            return this
        }

        fun flushInterval(flushInterval: Int): SegmentBuilder {
            this.flushInterval = flushInterval
            return this
        }

        fun flushSize(flushSize: Int): SegmentBuilder {
            this.flushSize = flushSize
            return this
        }

        fun log(logger: Logger): SegmentBuilder {
            this.logger = logger
            return this
        }

        fun networkExecutor(networkExecutor: ExecutorService): SegmentBuilder {
            this.networkExecutor = networkExecutor
            return this
        }

        fun build(): SegmentIntegration {
            if (context == null) {
                context = mockApplication()
                whenever(context!!.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE))
                    .thenReturn(PERMISSION_DENIED)
            }
            if (client == null) {
                client = mock(Client::class.java)
            }
            if (cartographer == null) {
                cartographer = Cartographer.INSTANCE
            }
            if (payloadQueue == null) {
                payloadQueue = mock(PayloadQueue::class.java)
            }
            if (stats == null) {
                stats = mock(Stats::class.java)
            }
            if (integrations == null) {
                integrations = emptyMap()
            }
            if (networkExecutor == null) {
                networkExecutor = SynchronousExecutor()
            }
            return SegmentIntegration(
                context,
                client,
                cartographer,
                networkExecutor,
                payloadQueue,
                stats,
                integrations,
                flushInterval.toLong(),
                flushSize,
                logger,
                Crypto.none(),
                DEFAULT_API_HOST
            )
        }
    }
}
