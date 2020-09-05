package com.segment.jsmiddleware

import com.segment.analytics.internal.Utils
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class ConnectionFactory {

    companion object {
        private const val DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000 // 20s
        private const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 15 * 1000 // 15s
        const val USER_AGENT = "analytics-android/" + BuildConfig.VERSION_NAME
    }

    @Throws(IOException::class)
    fun downloadEdgeFunctionBundle(url: String): Connection {
        val connection: HttpURLConnection = openConnection(url)
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            throw IOException("HTTP " + responseCode + ": " + connection.responseMessage)
        }
        return createGetConnection(connection)
    }

    @Throws(IOException::class)
    private fun createGetConnection(connection: HttpURLConnection): Connection {
        return object: Connection(connection, Utils.getInputStream(connection), null) {
            @Throws(IOException::class)
            override fun close() {
                super.close()
                inputStream?.close()
            }
        }
    }

    /**
     * Configures defaults for connections opened with [.upload], [ ][.attribution] and [.projectSettings].
     */
    @Throws(IOException::class)
    protected fun openConnection(url: String): HttpURLConnection {
        val requestedURL: URL
        requestedURL = try {
            URL(url)
        } catch (e: MalformedURLException) {
            throw IOException("Attempted to use malformed url: $url", e)
        }
        val connection = requestedURL.openConnection() as HttpURLConnection
        connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = DEFAULT_READ_TIMEOUT_MILLIS
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.doInput = true
        return connection
    }

    /**
     * Wraps an HTTP connection. Callers can either read from the connection via the [ ] or write to the connection via [OutputStream].
     */
    abstract class Connection(connection: HttpURLConnection?, inputStream: InputStream?, outputStream: OutputStream?) : Closeable {
        val connection: HttpURLConnection
        val inputStream: InputStream?
        val outputStream: OutputStream?

        @Throws(IOException::class)
        override fun close() {
            connection.disconnect()
        }

        init {
            requireNotNull(connection) { "connection == null" }
            this.connection = connection
            this.inputStream = inputStream
            this.outputStream = outputStream
        }
    }
}