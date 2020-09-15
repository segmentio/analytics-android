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

import android.net.Uri
import com.nhaarman.mockitokotlin2.whenever
import com.segment.analytics.internal.Private
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.squareup.okhttp.mockwebserver.RecordedRequest
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ClientTest {
    @Rule @JvmField val server = MockWebServer()
    @Rule @JvmField val folder = TemporaryFolder()
    private lateinit var client: Client
    private lateinit var mockClient: Client
    @Private lateinit var mockConnection: HttpURLConnection

    @Before
    fun setUp() {
        mockConnection = mock(HttpURLConnection::class.java)

        client = Client(
            "foo",
            object : ConnectionFactory() {
                @Throws(IOException::class)
                override fun openConnection(url: String): HttpURLConnection {
                    val path = Uri.parse(url).path
                    val mockServerURL = server.getUrl(path)
                    return super.openConnection(mockServerURL.toString())
                }
            }
        )

        mockClient = Client(
            "foo",
            object : ConnectionFactory() {
                @Throws(IOException::class)
                override fun openConnection(url: String): HttpURLConnection {
                    return mockConnection
                }
            }
        )
    }

    @Test
    @Throws(Exception::class)
    fun upload() {
        server.enqueue(MockResponse())

        val connection = client.upload()
        Assertions.assertThat(connection.os).isNotNull()
        Assertions.assertThat(connection.`is`).isNull()
        Assertions.assertThat(connection.connection.responseCode).isEqualTo(200) // consume the response
        RecordedRequestAssert.assertThat(server.takeRequest())
            .hasRequestLine("POST /v1/import HTTP/1.1")
            .containsHeader("User-Agent", ConnectionFactory.USER_AGENT)
            .containsHeader("Content-Type", "application/json")
            .containsHeader("Content-Encoding", "gzip")
            .containsHeader("Authorization", "Basic Zm9vOg==")
    }

    @Test
    @Throws(Exception::class)
    fun attribution() {
        server.enqueue(MockResponse())

        val connection = client.attribution()
        Assertions.assertThat(connection.os).isNotNull()
        Assertions.assertThat(connection.`is`).isNull()
        Assertions.assertThat(connection.connection.responseCode).isEqualTo(200) // consume the response
        RecordedRequestAssert.assertThat(server.takeRequest())
            .hasRequestLine("POST /v1/attribution HTTP/1.1")
            .containsHeader("User-Agent", ConnectionFactory.USER_AGENT)
            .containsHeader("Content-Type", "application/json")
            .containsHeader("Authorization", "Basic Zm9vOg==")
    }

    @Test
    @Throws(Exception::class)
    fun closingUploadConnectionClosesStreams() {
        val os = mock(OutputStream::class.java)
        whenever(mockConnection.outputStream).thenReturn(os)
        whenever(mockConnection.responseCode).thenReturn(200)

        val connection = mockClient.upload()
        Mockito.verify(mockConnection).doOutput = true
        Mockito.verify(mockConnection).setChunkedStreamingMode(0)

        connection.close()
        Mockito.verify(mockConnection).disconnect()
        Mockito.verify(os).close()
    }

    @Test
    @Throws(Exception::class)
    fun closingUploadConnectionClosesStreamsForNon200Response() {
        val os = mock(OutputStream::class.java)
        whenever(mockConnection.outputStream).thenReturn(os)
        whenever(mockConnection.responseCode).thenReturn(202)

        val connection = mockClient.upload()
        Mockito.verify(mockConnection).doOutput = true
        Mockito.verify(mockConnection).setChunkedStreamingMode(0)

        connection.close()
        Mockito.verify(mockConnection).disconnect()
        Mockito.verify(os).close()
    }

    @Test
    @Throws(Exception::class)
    fun uploadFailureClosesStreamsAndThrowsException() {
        val os = mock(OutputStream::class.java)
        val `is` = mock(InputStream::class.java)
        whenever(mockConnection.outputStream).thenReturn(os)
        whenever(mockConnection.responseCode).thenReturn(300)
        whenever(mockConnection.responseMessage).thenReturn("bar")
        whenever(mockConnection.inputStream).thenReturn(`is`)

        val connection = mockClient.upload()
        Mockito.verify(mockConnection).doOutput = true
        Mockito.verify(mockConnection).setChunkedStreamingMode(0)

        try {
            connection.close()
            Assertions.fail(">= 300 return code should throw an exception")
        } catch (e: Client.HTTPException) {
            Assertions.assertThat(e)
                .hasMessage(
                    "HTTP 300: bar. " +
                        "Response: Could not read response body for rejected message: " +
                        "java.io.IOException: Underlying input stream returned zero bytes"
                )
        }
        Mockito.verify(mockConnection).disconnect()
        Mockito.verify(os).close()
    }

    @Test
    @Throws(Exception::class)
    fun uploadFailureWithErrorStreamClosesStreamsAndThrowsException() {
        val os = mock(OutputStream::class.java)
        val `is` = mock(InputStream::class.java)
        whenever(mockConnection.outputStream).thenReturn(os)
        whenever(mockConnection.responseCode).thenReturn(404)
        whenever(mockConnection.responseMessage).thenReturn("bar")
        whenever(mockConnection.inputStream).thenThrow(FileNotFoundException())
        whenever(mockConnection.errorStream).thenReturn(`is`)

        val connection = mockClient.upload()
        Mockito.verify(mockConnection).doOutput = true
        Mockito.verify(mockConnection).setChunkedStreamingMode(0)

        try {
            connection.close()
            Assertions.fail(">= 300 return code should throw an exception")
        } catch (e: Client.HTTPException) {
            Assertions.assertThat(e)
                .hasMessage(
                    "HTTP 404: bar. " +
                        "Response: Could not read response body for rejected message: " +
                        "java.io.IOException: Underlying input stream returned zero bytes"
                )
        }
        Mockito.verify(mockConnection).disconnect()
        Mockito.verify(os).close()
    }

    @Test
    @Throws(Exception::class)
    fun fetchSettings() {
        server.enqueue(MockResponse())

        val connection = client.fetchSettings()
        Assertions.assertThat(connection.os).isNull()
        Assertions.assertThat(connection.`is`).isNotNull()
        Assertions.assertThat(connection.connection.responseCode).isEqualTo(200)
        RecordedRequestAssert.assertThat(server.takeRequest())
            .hasRequestLine("GET /v1/projects/foo/settings HTTP/1.1")
            .containsHeader("User-Agent", ConnectionFactory.USER_AGENT)
            .containsHeader("Content-Type", "application/json")
    }

    @Test
    @Throws(Exception::class)
    fun fetchSettingsFailureClosesStreamsAndThrowsException() {
        whenever(mockConnection.responseCode).thenReturn(204)
        whenever(mockConnection.responseMessage)
            .thenReturn("no cookies for you http://bit.ly/1EMHBNb")

        try {
            mockClient.fetchSettings()
            Assertions.fail("Non 200 return code should throw an exception")
        } catch (e: IOException) {
            Assertions.assertThat(e).hasMessage("HTTP " + 204 + ": no cookies for you http://bit.ly/1EMHBNb")
        }
        Mockito.verify(mockConnection).disconnect()
    }

    @Test
    @Throws(Exception::class)
    fun closingFetchSettingsClosesStreams() {
        val `is` = mock(InputStream::class.java)
        whenever(mockConnection.inputStream).thenReturn(`is`)
        whenever(mockConnection.responseCode).thenReturn(200)

        val connection = mockClient.fetchSettings()

        connection.close()
        Mockito.verify(mockConnection).disconnect()
        Mockito.verify(`is`).close()
    }

    internal class RecordedRequestAssert constructor(actual: RecordedRequest) :
        AbstractAssert<RecordedRequestAssert,
            RecordedRequest>(actual, RecordedRequestAssert::class.java) {

        fun containsHeader(name: String, expectedHeader: String): RecordedRequestAssert {
            isNotNull
            val actualHeader = actual.getHeader(name)
            Assertions.assertThat(actualHeader)
                .overridingErrorMessage(
                    "Expected header <%s> to be <%s> but was <%s>.", name, expectedHeader, actualHeader
                )
                .isEqualTo(expectedHeader)
            return this
        }

        fun containsHeader(name: String): RecordedRequestAssert {
            isNotNull
            val actualHeader = actual.getHeader(name)
            Assertions.assertThat(actualHeader)
                .overridingErrorMessage(
                    "Expected header <%s> to not be empty but was.", name, actualHeader
                )
                .isNotNull()
                .isNotEmpty()
            return this
        }

        fun hasRequestLine(requestLine: String): RecordedRequestAssert {
            isNotNull
            val actualRequestLine = actual.requestLine
            Assertions.assertThat(actualRequestLine)
                .overridingErrorMessage(
                    "Expected requestLine <%s> to be <%s> but was not.", actualRequestLine, requestLine
                )
                .isEqualTo(requestLine)
            return this
        }

        companion object {
            fun assertThat(recordedRequest: RecordedRequest): RecordedRequestAssert {
                return RecordedRequestAssert(recordedRequest)
            }
        }
    }
}
