package com.segment.jsmiddleware

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.eclipsesource.v8.V8Array
import com.segment.analytics.JSMiddleware
import com.segment.jsruntime.JSRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
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

    companion object {
        private const val INTEGRATIONS_KEY = "integrations"
        private const val VERSION_KEY = "version"
        private const val DOWNLOAD_URL_KEY = "downloadURL"
        private const val EDGEFUNCTION_FILENAME = "edgeFunction.js"
    }

    init {
        this.fallbackFile = localFile
        this.executor = Executors.newSingleThreadExecutor()

        // Start download of url
        val middlewareProcessor = EdgeFunctionMiddlewareProcessor("", this)

        try {
//            middlewareProcessor.downloadFileAsync(context, "https://cdn.edgefn.segment.com/x3jn1hFHarPWw2NSpKhSmh/3bb2850b-559d-491e-9b27-6d0a1520da77.js")
            fallbackFile?.let {
                middlewareProcessor.configureLocalFile(context, it);
            }
        } catch (e: Exception) {
            Log.d("JSMiddleware", "Could not download edge functions")
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
            val source = runtime?.getArray("sourceMiddleware")
            val destination = runtime?.getObject("destinationMiddleware") // This needs to be a map, not array
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
}