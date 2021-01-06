package com.segment.analytics.platform.extensions

import android.util.Log
import com.segment.analytics.ValueMap
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload
import com.segment.analytics.internal.Utils
import com.segment.analytics.platform.DestinationExtension
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ExecutorService

// Webhook Destination configured as an extension. Can be used for viewing payloads and debugging.
class WebhookExtension(private val webhookUrl: String, private val networkExecutor: ExecutorService): DestinationExtension() {
    override val name: String = "WebhookDestination-$webhookUrl"

    /**
     * Sends a JSON payload to the specified webhookUrl, with the Content-Type=application/json
     * header set
     */
    private fun sendPayloadToWebhook(payload: BasePayload?) = networkExecutor.submit {
        payload?.let {
            Log.d("Analytics", "Running ${payload.getString("type")} payload through $name")
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
                    Log.w("Analytics", "Failed to send payload, statusCode=$responseCode, body=$responseBody")
                }
            }
        }
    }

    override fun track(payload: TrackPayload?): BasePayload? {
        sendPayloadToWebhook(payload)
        return payload
    }

    override fun identify(payload: IdentifyPayload?): BasePayload? {
        sendPayloadToWebhook(payload)
        return payload
    }

    override fun screen(payload: ScreenPayload?): BasePayload? {
        sendPayloadToWebhook(payload)
        return payload
    }

    override fun group(payload: GroupPayload?): BasePayload? {
        sendPayloadToWebhook(payload)
        return payload
    }

    override fun alias(payload: AliasPayload?): BasePayload? {
        sendPayloadToWebhook(payload)
        return payload
    }

    override fun flush() {

    }

    override fun reset() {

    }
}