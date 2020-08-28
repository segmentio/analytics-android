package com.segment.jsmiddleware

import android.content.Context
import android.util.Log
import okhttp3.*
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import kotlin.jvm.Throws

class EdgeFunctionMiddlewareProcessor(private val writeKey: String, private val listener: EdgeFunctionMiddlewareProcessorListener) {
    private val client = OkHttpClient()

    ////
    // Public Methods
    @Throws(Exception::class)
    fun configureLocalFile(mobContext: Context, localFile: String) {
        val localJSMiddlewareInputStream = mobContext.assets.open(localFile)
        val result = IOUtils.toString(localJSMiddlewareInputStream, StandardCharsets.UTF_8)
        listener.jsRetrieved(result)
    }

    @Throws(Exception::class)
    fun downloadFileAsync(mobContext: Context, downloadUrl: String?) {

        // Grab settings first
//        try {
//            val settings = fetchSettings()
//        } catch (e: Exception) {
//            Log.e("JSMiddleware", "Could not parse settings")
//        }
        val request = Request.Builder().url(downloadUrl!!).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    throw IOException("Failed to download file: $response")
                }
                try {
                    response.body.use { responseBody ->

                        // Write the file out
                        if (responseBody != null) {
                            val fos = FileOutputStream(jsDownloadFullPath(mobContext, "blah.js"))
                            fos.write(responseBody.bytes())
                            fos.close()
                        }

                        // Let the executor know the file has been saved
                        listener.jsRetrieved(getLocalFile(mobContext))
                    }
                } catch (e: Exception) {
                    Log.e("JSMiddleware", "Could not parse file body")
                }
            }
        })
    }

    ////
    // Private Methods
    @Throws(Exception::class)
    private fun fetchSettings(): EdgeFunctionMiddlewareSettings {
        val client = OkHttpClient()
        val request = Request.Builder()
                .url("https://cdn-settings.segment.com/v1/projects/EEe2Yv2c4HC2da9FpFeJnBoSxgmkrNVD/settings")
                .build()
        val response = client.newCall(request).execute()
        val settingsJSON = response.body!!.string()
        val jsonObject = JSONObject(settingsJSON)
        val edgeFunctionObject = jsonObject.getJSONObject("edgeFunction")
        val downloadURLString = edgeFunctionObject.getString("downloadURL")
        val version = edgeFunctionObject.getInt("version")
        return EdgeFunctionMiddlewareSettings(version, downloadURLString)
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

class EdgeFunctionMiddlewareSettings(var version: Int, var downloadURL: String)