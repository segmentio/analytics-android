package com.segment.jsmiddleware

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.eclipsesource.v8.V8Array
import com.segment.analytics.Cartographer
import com.segment.analytics.JSMiddleware
import com.segment.analytics.ValueMap
import com.segment.analytics.internal.Utils
import com.segment.jsruntime.JSRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EdgeFunctionMiddleware(context: Context, localFile: String?) : JSMiddleware(context), EdgeFunctionMiddlewareProcessorListener {

    @Volatile
    private var jsSourceMiddleware: V8Array? = null

    @Volatile
    private var jsDestinationMiddleware: V8Array? = null
    private var runtime: JSRuntime? = null
    private var bundleStream: InputStream? = null
    private var fallbackFile: String? = null
    private val executor: ExecutorService
    private val configuration: Cache
    private val middlewareProcessor: EdgeFunctionMiddlewareProcessor

    companion object {
        private const val INTEGRATIONS_KEY = "integrations"
        private const val VERSION_KEY = "version"
        private const val DOWNLOAD_URL_KEY = "downloadURL"
        private const val EDGE_FUNCTION_FILENAME = "edgeFunction.js"
    }

    init {
        this.fallbackFile = localFile
        this.executor = Executors.newSingleThreadExecutor()
        this.configuration = Cache(context, Cartographer.Builder().build(), "123")

        // Start download of url
        middlewareProcessor = EdgeFunctionMiddlewareProcessor(this)

        try {
            if (middlewareProcessor.localFileExists(context)) {
                middlewareProcessor.configureLocalFile(context)
            } else {
                fallbackFile?.let {
                    middlewareProcessor.configureFallbackFile(context, it);
                }
            }
        } catch (e: Exception) {
            Log.d("JSMiddleware", "Could not download edge functions")
        }
    }

    override fun setEdgeFunctionData(newConfig: ValueMap?) {
        if (newConfig != null && newConfig.size != 0 && newConfig.containsKey(VERSION_KEY) && newConfig.containsKey(DOWNLOAD_URL_KEY)) {
            val currentConfig = configuration.get()
            if (currentConfig != null) {
                // update if newer
                if (newConfig.getInt(VERSION_KEY, 0) > currentConfig.getInt(VERSION_KEY, 0)) {
                    updateConfig(newConfig)
                }
            } else {
                // we dont have the config, so set it
                updateConfig(newConfig)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun jsRetrieved(data: String?) = runBlocking<Unit> {

        try {
            bundleStream = ByteArrayInputStream(data!!.toByteArray(StandardCharsets.UTF_8))
            Log.i("JSMiddleware", "Loaded bundleStream")

            // Set up the runners that may be configured now that we have the data from local or web
            val runnerJob = GlobalScope.launch(Dispatchers.IO) {
                configureRunners()
            }
            runnerJob.join()
        } catch (e: Exception) {
            Log.e("JSMiddleware", "Could not parse edge functions")
        }
    }

    private fun configureRunners() {
        // Make sure to initialize the middleware on the same thread it is used on since V8 is
        // single threaded and will blow up. Also verify there is a bundleStream, otherwise bail.
        if ((jsDestinationMiddleware == null && jsSourceMiddleware == null) && bundleStream != null) {

            runtime = JSRuntime(bundleStream)
            Log.d("JSMiddleware", bundleStream.toString())

            // Check for Source Middleware
            val source = runtime?.getArray("edge_function.sourceMiddleware")
            val destination = runtime?.getObject("edge_function.destinationMiddleware") // This needs to be a map, not array
            if (source != null) {
                jsSourceMiddleware = source

                // Now replace sourceMiddleware
                runtime?.let {
                    val copiedRuntime = it
                    jsSourceMiddleware?.let {
                        val copiedJSSourceMiddleware = it
                        val sourceRunner = EdgeFunctionRunner(copiedRuntime, copiedJSSourceMiddleware)
                        val sourceRunnerList = listOf(sourceRunner)
                        val currentThreadID = Thread.currentThread().id
                        Log.d("Thread", currentThreadID.toString())
                        sourceMiddleware = sourceRunnerList // Create runner that adheres to Middleware jsSourceMiddleware
                    }
                }
            }


            // Now for destination, which is a map instead of a list
//            if (destination != null) {
//                jsDestinationMiddleware = destination
//            }
        } else if (bundleStream == null) {
            return
        }
    }

    private fun updateConfig(settings: ValueMap) {
        val downloadURL = settings.getString(DOWNLOAD_URL_KEY)
        executor.submit {
            middlewareProcessor.downloadFile(context, downloadURL)
            configuration.set(settings) // save to cache
        }
    }

    internal class Cache(context: Context, private val cartographer: Cartographer, tag: String?) {
        private val preferences: SharedPreferences = Utils.getSegmentSharedPreferences(context, tag)
        private val key: String = "edge-functions-$tag"
        private var value: ValueMap? = null
        fun get(): ValueMap? {
            if (value == null) {
                val json = preferences.getString(key, null)
                if (Utils.isNullOrEmpty(json)) return null
                value = (return try {
                    val map = cartographer.fromJson(json)
                    ValueMap(map)
                } catch (ignored: IOException) {
                    null
                })
            }
            return value
        }

        val isSet: Boolean
            get() = preferences.contains(key)

        fun set(value: ValueMap) {
            this.value = value
            val json = cartographer.toJson(value)
            preferences.edit().putString(key, json).apply()
        }
    }

}