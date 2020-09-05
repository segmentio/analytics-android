package com.segment.jsmiddleware

import android.content.Context
import android.util.Log
import com.segment.analytics.internal.Utils
import org.apache.commons.io.IOUtils
import java.io.*
import java.nio.charset.Charset

class EdgeFunctionMiddlewareProcessor(private val listener: EdgeFunctionMiddlewareProcessorListener) {

    @Throws(Exception::class)
    fun configureLocalFile(mobContext: Context) {
        listener.jsRetrieved(getLocalFile(mobContext))
    }

    @Throws(Exception::class)
    fun configureFallbackFile(mobContext: Context, localFile: String) {
        val localJSMiddlewareInputStream = mobContext.assets.open(localFile)
        val result = IOUtils.toString(localJSMiddlewareInputStream, Charset.forName("UTF-8"))
        listener.jsRetrieved(result)
    }

    @Throws(Exception::class)
    fun downloadFile(mobContext: Context, downloadUrl: String) {
        var connection: ConnectionFactory.Connection? = null
        try {
            connection = ConnectionFactory().downloadEdgeFunctionBundle(downloadUrl)
            if (connection.inputStream != null) {
                val inputStream = connection.inputStream!!
                val function = IOUtils.toString(inputStream, Charset.forName("UTF-8"))
                Log.d("PRAY", function)

                Log.d("JSMiddleware", "New Edge Function downloaded, will be used on next app restart")

                val fos = FileOutputStream(jsDownloadFullPath(mobContext, "blah.js"))
                fos.write(function.toByteArray())
                fos.close()
                // Let the executor know the file has been saved
                //  listener.jsRetrieved(getLocalFile(mobContext)) // no need to notify since we dont hot-swap
            }
        } catch (e: Exception) {
            Log.e("JSMiddleware", "Could not parse file body")
        } finally {
            Utils.closeQuietly(connection)
        }
    }

    fun localFileExists(mobContext: Context): Boolean {
        return jsDownloadFullPath(mobContext, "").exists()
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

    // Get the local file that was saved off from a previous download
    private fun getLocalFile(mobContext: Context): String {
        val fullPath = jsDownloadFullPath(mobContext, null)
        val text = StringBuilder()
        try {
            val reader = BufferedReader(FileReader(fullPath))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                text.append(line)
                text.append("\n")
            }
            reader.close()
        } catch (exception: IOException) {
            Log.e("JSMiddleware", "Could not find file")
        }
        return text.toString()
    }
}