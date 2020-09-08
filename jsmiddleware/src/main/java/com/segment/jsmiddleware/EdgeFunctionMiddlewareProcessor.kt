package com.segment.jsmiddleware

import android.content.Context
import android.util.Log
import com.segment.analytics.internal.Utils as SegmentUtils
import java.io.*

class EdgeFunctionMiddlewareProcessor(private val listener: EdgeFunctionMiddlewareProcessorListener) {

    @Throws(Exception::class)
    fun configureCachedFile(mobContext: Context) {
        listener.jsRetrieved(getCachedFileContents(mobContext))
    }

    @Throws(Exception::class)
    fun configureFallbackFile(mobContext: Context, localFile: String) {
        val localJSMiddlewareInputStream = mobContext.assets.open(localFile)
        val result = readInputStream(localJSMiddlewareInputStream)
        listener.jsRetrieved(result)
    }

    @Throws(Exception::class)
    fun downloadFile(mobContext: Context, downloadUrl: String) {
        var connection: ConnectionUtils.Connection? = null
        try {
            connection = downloadEdgeFunctionBundle(downloadUrl)
            connection.inputStream?.let {
                val function = readInputStream(it)

                Log.d("JSMiddleware", "New Edge Function downloaded, will be used on next app restart")

                val fos = FileOutputStream(jsDownloadFullPath(mobContext, "blah.js"))
                fos.write(function.toByteArray())
                fos.close()
            }
        } catch (e: Exception) {
            Log.e("JSMiddleware", "Could not parse file body")
        } finally {
            SegmentUtils.closeQuietly(connection)
        }
    }

    // Check if local cached file exists
    fun cachedFileExists(mobContext: Context): Boolean {
        return jsDownloadFullPath(mobContext, null).exists()
    }

    // Grab the local directory to save the js file
    private fun jsDownloadDirectory(mobContext: Context): File {
        val returnDirectory = File(mobContext.filesDir, "segmentJSBundles")
        if (!returnDirectory.exists()) {
            returnDirectory.mkdir()
        }
        return returnDirectory
    }

    private fun jsDownloadFullPath(mobContext: Context, filename: String?): File {
        return File(jsDownloadDirectory(mobContext).toString(), "jsMiddleware.js")
    }

    // Get the local cached file contents that was saved off from a previous download
    private fun getCachedFileContents(mobContext: Context): String {
        val fullPath = jsDownloadFullPath(mobContext, null)
        return readInputStream(FileInputStream(fullPath))
    }
}