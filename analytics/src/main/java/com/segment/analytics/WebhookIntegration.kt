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

import android.util.Log
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.Integration
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload
import com.segment.analytics.internal.Utils
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ExecutorService

internal class WebhookIntegration(
    private val webhookUrl: String,
    private val tag: String,
    private val integrationKey: String,
    private val networkExecutor: ExecutorService
) : Integration<Unit>() {

    /**
     * Sends a JSON payload to the specified webhookUrl, with the Content-Type=application/json
     * header set
     */
    private fun sendPayloadToWebhook(payload: ValueMap) = networkExecutor.submit {
        Log.d(tag, "Running ${payload.getString("type")} payload through $integrationKey")
        val requestedURL: URL
        requestedURL = try {
            URL(webhookUrl)
        } catch (e: MalformedURLException) {
            throw IOException("Attempted to use malformed url: $webhookUrl", e)
        }

        val connection = requestedURL.openConnection() as HttpURLConnection
        connection.doOutput = true
        connection.setChunkedStreamingMode(0)
        connection.setRequestProperty("Content-Type", "application/json")

        val outputStream = DataOutputStream(connection.outputStream)
        val payloadJson = payload.toJsonObject().toString()
        outputStream.writeBytes(payloadJson)

        outputStream.use {
            val responseCode = connection.responseCode
            if (responseCode >= 300) {
                var responseBody: String?
                var inputStream: InputStream? = null
                try {
                    inputStream = Utils.getInputStream(connection)
                    responseBody = Utils.readFully(inputStream)
                } catch (e: IOException) {
                    responseBody = (
                        "Could not read response body for rejected message: " +
                            e.toString()
                        )
                } finally {
                    inputStream?.close()
                }
                Log.w(tag, "Failed to send payload, statusCode=$responseCode, body=$responseBody")
            }
        }
    }

    override fun identify(identify: IdentifyPayload) {
        sendPayloadToWebhook(identify)
    }

    override fun group(group: GroupPayload) {
        sendPayloadToWebhook(group)
    }

    override fun track(track: TrackPayload) {
        sendPayloadToWebhook(track)
    }

    override fun alias(alias: AliasPayload) {
        sendPayloadToWebhook(alias)
    }

    override fun screen(screen: ScreenPayload) {
        sendPayloadToWebhook(screen)
    }
}

class WebhookIntegrationFactory(private val key: String, private val webhookUrl: String) :
    Integration.Factory {
    override fun create(settings: ValueMap?, analytics: Analytics): Integration<Unit> {
        return WebhookIntegration(
            webhookUrl = webhookUrl,
            tag = "Analytics/${analytics.tag}",
            integrationKey = key(),
            networkExecutor = analytics.networkExecutor
        )
    }

    override fun key(): String {
        return "webhook_$key"
    }
}
