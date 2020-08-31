package com.segment.jsmiddleware

import android.util.Log
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8Value
import com.eclipsesource.v8.utils.V8ObjectUtils
import com.segment.analytics.Middleware
import com.segment.analytics.ValueMap
import com.segment.analytics.integrations.*
import com.segment.jsruntime.JSRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

class EdgeFunctionRunner(runtime: JSRuntime, edgeMiddleware: V8Array): Middleware {

    companion object {
        private const val INTEGRATIONS_KEY = "integrations"
    }

    var runtime: JSRuntime? = null

    @Volatile
    var edgeMiddleware: V8Array? = null

    init {
        this.runtime = runtime
        this.edgeMiddleware = edgeMiddleware
    }

    /**
     * Internal function to parse javascript as native methods.
     */
    fun manualRun(event: HashMap<String, Any?>, middleware: V8Array): HashMap<String, Any?>? {

        if (runtime != null && middleware.length() > 0) {
            val result = HashMap(event)
            Log.d("LENGTH", middleware.length().toString())
            for (n in 0 until middleware.length()) {
                val fnObject = middleware.getObject(n)
                // make sure we have an actual function like it's supposed to be...
                if (fnObject.v8Type == V8Value.V8_FUNCTION) {
                    val fn = fnObject as V8Function
                    // setup the params to pass in...
                    val params = V8Array(runtime!!.runtime)
                    params.push(V8ObjectUtils.toV8Object(runtime!!.runtime, result))
                    // call it and pick up the result
                    val fnResult = fn.call(null, params) as V8Object
                    // convert the function's result back to a HashMap...
                    val newResult: Map<String, *> = V8ObjectUtils.toMap(fnResult)
                    result.clear()
                    result.putAll(newResult)
                }
            }
            if (!result.isEmpty()) {
                return result
            }
        }
        return null
    }

    override fun intercept(chain: Middleware.Chain) = runBlocking<Unit> {

        // Make sure to initialize the middleware on the same thread it is used on since V8 is
        // single threaded and will blow up. Also verify there is a bundleStream, otherwise bail.

        val currentThreadID = Thread.currentThread().id
        Log.d("Thread", currentThreadID.toString())

        var payload = chain.payload()
        var integrations = payload.integrations()
        val eventData: HashMap<String, Any?> = HashMap(integrations)

        // Build up the data ()
        val userId = payload.userId()
        if (userId != null) {
            eventData["userId"] = userId
        }
        eventData["anonymousId"] = payload.anonymousId()
        eventData["messageId"] = payload.messageId()

        eventData["integrations"] = payload.integrations()
        eventData["context"] = payload.context()
        when (payload.type()) {
            BasePayload.Type.identify -> {
                val identifyPayload = payload as IdentifyPayload
                eventData["traits"] = identifyPayload.traits()
            }
            BasePayload.Type.track -> {
                val trackPayload = payload as TrackPayload
                eventData["name"] = trackPayload.event()
                eventData["properties"] = trackPayload.properties()
            }
            BasePayload.Type.screen -> {
                val screenPayload = payload as ScreenPayload
                eventData["properties"] = screenPayload.properties()

                val name = screenPayload.name()
                if (name != null) {
                    eventData["name"] = name
                }
            }
            BasePayload.Type.alias -> {
                val aliasPayload = payload as AliasPayload
                val aliasUserId = aliasPayload.userId()
                if (aliasUserId != null) {
                    eventData["newId"] = aliasUserId // userId is newId and previousId is previous??
                }
            }
            BasePayload.Type.group -> {
                val groupPayload = payload as GroupPayload
                eventData["groupId"] = groupPayload.groupId()
                eventData["traits"] = groupPayload.traits()
            }
        }
        Log.d("BEFORE Edge Functions", eventData.toString())
        var parsedEventData: HashMap<String, Any?>? = eventData

        val interceptJob = GlobalScope.launch(Dispatchers.IO) {
            try {
                edgeMiddleware?.let { edge ->
                    parsedEventData?.let { data ->
                        parsedEventData = manualRun(data, edge)
                    }
                }
            } catch (e: Exception) {
                Log.d("EXCEPTION", e.toString())
            }
        }
        interceptJob.join()

        Log.d("AFTER Edge Functions", parsedEventData.toString())
        if (parsedEventData == null || emptyMap<Any, Any>() == parsedEventData) {
            // Empty map does not allow to put values.
            integrations = ValueMap()
        } else {
            // Sometimes integrations is a unmodifiable map if the event is constructed with a Builder.
            // Sadly, we don't know exactly when that is, so this Middleware need to copy the integrations
            // map for each event.
            integrations = ValueMap(parsedEventData)
        }
//        payload.putValue(INTEGRATIONS_KEY, integrations)
        payload = chain.payload().toBuilder().context(integrations).build()


        MainScope().launch {
            chain.proceed(payload)
        }
    }

}