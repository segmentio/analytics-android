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

import android.Manifest
import com.google.common.util.concurrent.MoreExecutors
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.Integration
import com.segment.analytics.integrations.TrackPayload
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyObject
import org.mockito.Mock
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@Ignore("failing tests. need to fix")
class DestinationMiddlewareTest {

    lateinit var builder: Analytics.Builder

    @Mock
    lateinit var integrationFoo: Integration<Void>

    @Mock
    lateinit var integrationBar: Integration<Void>

    @Before
    fun setUp() {
        initMocks(this)
        Analytics.INSTANCES.clear()
        TestUtils.grantPermission(RuntimeEnvironment.application, Manifest.permission.INTERNET)
        val projectSettings =
            ValueMap()
                .putValue(
                    "integrations",
                    ValueMap()
                        .putValue("foo", ValueMap().putValue("appToken", "foo_token"))
                        .putValue(
                            "bar",
                            ValueMap()
                                .putValue("appToken", "foo_token")
                                .putValue("trackAttributionData", true)
                        )
                )
        builder =
            Analytics.Builder(RuntimeEnvironment.application, "write_key")
                .defaultProjectSettings(projectSettings)
                .use(
                    object : Integration.Factory {
                        override fun create(settings: ValueMap, analytics: Analytics): Integration<*>? {
                            return integrationFoo
                        }

                        override fun key(): String {
                            return "foo"
                        }
                    })
                .use(
                    object : Integration.Factory {
                        override fun create(settings: ValueMap, analytics: Analytics): Integration<*>? {
                            return integrationBar
                        }

                        override fun key(): String {
                            return "bar"
                        }
                    })
                .executor(MoreExecutors.newDirectExecutorService())
    }

    @Test
    @Throws(Exception::class)
    fun middlewareRuns() {
        val payloadRef = AtomicReference<TrackPayload>()
        val analytics = builder
            .useDestinationMiddleware(
                "foo"
            ) { chain ->
                payloadRef.set(chain.payload() as TrackPayload)
                chain.proceed(chain.payload())
            }
            .build()
        analytics.track("foo")
        assertThat(payloadRef.get().event()).isEqualTo("foo")
        verify(integrationFoo).track(payloadRef.get())
    }

    @Test
    @Throws(Exception::class)
    fun middlewareDoesNotRunForOtherIntegration() {
        val payloadRefOriginal = AtomicReference<TrackPayload>()
        val payloadRefDestMiddleware = AtomicReference<TrackPayload>()
        val analytics = builder
            .useSourceMiddleware { chain ->
                payloadRefOriginal.set(chain.payload() as TrackPayload)
                chain.proceed(chain.payload())
            }
            .useDestinationMiddleware(
                "foo"
            ) { chain -> // modify reference and add new property
                val payload = chain.payload() as TrackPayload
                val properties = ValueMap()
                properties.putAll(payload.properties())
                val newPayload = payload
                    .toBuilder()
                    .properties(properties.putValue("middleware_key", "middleware_value"))
                    .build()
                payloadRefDestMiddleware.set(newPayload)
                chain.proceed(newPayload)
            }
            .build()

        analytics.track("foo")

        assertThat(payloadRefOriginal.get().event()).isEqualTo("foo")
        verify(integrationBar).track(payloadRefOriginal.get())

        assertThat(payloadRefDestMiddleware.get().event()).isEqualTo("foo")
        assertThat(payloadRefDestMiddleware.get().properties()).containsKey("middleware_key")
        assertThat(payloadRefDestMiddleware.get().properties()["middleware_key"])
            .isEqualTo("middleware_value")
        verify(integrationFoo).track(payloadRefDestMiddleware.get())
    }

    @Test
    @Throws(Exception::class)
    fun middlewareWillRunForMultipleIntegrations() {
        val payloadRefOriginal = AtomicReference<TrackPayload>()
        val payloadRefDestMiddleware = AtomicReference<TrackPayload>()
        val destCounter = AtomicInteger(0)
        val middleware = Middleware { chain ->
            val newPayload = chain.payload().toBuilder().build() as TrackPayload
            payloadRefDestMiddleware.set(newPayload)
            destCounter.incrementAndGet()
            chain.proceed(newPayload)
        }
        val analytics = builder
            .useSourceMiddleware { chain ->
                payloadRefOriginal.set(chain.payload() as TrackPayload)
                chain.proceed(chain.payload())
            }
            .useDestinationMiddleware("foo", middleware)
            .useDestinationMiddleware("Segment.io", middleware)
            .build()

        analytics.track("foo")

        assertThat(destCounter.get()).isEqualTo(2) // should only be called for 2 integrations
        verify(integrationBar).track(payloadRefOriginal.get())
        verify(integrationFoo).track(payloadRefDestMiddleware.get())
    }

    @Test
    @Throws(Exception::class)
    fun middlewareCanShortCircuit() {
        val payloadRef = AtomicReference<TrackPayload>()
        val analytics = builder
            .useSourceMiddleware { chain ->
                payloadRef.set(chain.payload() as TrackPayload)
                chain.proceed(chain.payload())
            }
            .useDestinationMiddleware(
                "foo"
            ) {
                // drop event for `foo` integration
            }
            .build()

        analytics.track("foo")

        assertThat(payloadRef.get().event()).isEqualTo("foo")
        verify(integrationFoo, never()).track(anyObject()) // validate event does not go through
        verify(integrationBar).track(payloadRef.get()) // validate event goes through
    }

    @Test
    @Throws(Exception::class)
    fun middlewareCanChain() {
        val payloadRef = AtomicReference<TrackPayload>()
        val analytics = builder
            .useDestinationMiddleware(
                "foo"
            ) { chain ->
                val payload = chain.payload() as TrackPayload
                val properties = ValueMap()
                properties.putAll(payload.properties())
                val newPayload = payload.toBuilder().properties(properties.putValue("key1", "val1")).build()
                chain.proceed(newPayload)
            }
            .useDestinationMiddleware(
                "foo"
            ) { chain ->
                val payload = chain.payload() as TrackPayload
                val properties = ValueMap()
                properties.putAll(payload.properties())
                val newPayload = payload.toBuilder().properties(properties.putValue("key2", "val2")).build()
                payloadRef.set(newPayload)
                chain.proceed(newPayload)
            }
            .build()

        analytics.track("foo")
        assertThat(payloadRef.get().properties()).containsKey("key1")
        assertThat(payloadRef.get().properties()["key1"]).isEqualTo("val1")
        assertThat(payloadRef.get().properties()).containsKey("key2")
        assertThat(payloadRef.get().properties()["key2"]).isEqualTo("val2")
    }

    @Test
    @Throws(Exception::class)
    fun middlewareCanTransform() {
        val payloadRefOriginal = AtomicReference<IdentifyPayload>()
        val payloadRefDestMiddleware = AtomicReference<IdentifyPayload>()
        val analytics = builder
            .useSourceMiddleware { chain ->
                payloadRefOriginal.set(chain.payload() as IdentifyPayload)
                chain.proceed(chain.payload())
            }
            .useDestinationMiddleware(
                "foo"
            ) { chain -> chain.proceed(chain.payload().toBuilder().messageId("override").build()) }
            .useDestinationMiddleware(
                "foo"
            ) { chain ->
                val payload = chain.payload()
                payloadRefDestMiddleware.set(payload as IdentifyPayload)
                chain.proceed(payload)
            }
            .build()

        analytics.identify("prateek")
        assertThat(payloadRefDestMiddleware.get().messageId()).isEqualTo("override")
        verify(integrationFoo).identify(payloadRefDestMiddleware.get())
        verify(integrationBar).identify(payloadRefOriginal.get())
    }

    /** Sample Middleware Tests * */
    @Test
    @Throws(Exception::class)
    fun middlewareAddProp() {
        // Add a simple key-value to the properties
        val analytics = builder
            .useDestinationMiddleware(
                "foo"
            ) { chain -> // Add step:1 to properties
                var payload = chain.payload()
                if (payload.type() == BasePayload.Type.track) {
                    val track = payload as TrackPayload
                    if (track.event() == "checkout started") {
                        val newProps = ValueMap()
                        newProps.putAll(track.properties())
                        newProps["step"] = 1
                        payload = track.toBuilder().properties(newProps).build()
                    }
                }
                chain.proceed(payload)
            }
            .build()
        analytics.track("checkout started")
        val fooTrack = ArgumentCaptor.forClass(TrackPayload::class.java)
        verify(integrationFoo).track(fooTrack.capture())
        assertThat(fooTrack.value.properties()).containsKey("step")
        assertThat(fooTrack.value.properties()["step"]).isEqualTo(1)
    }

    @Test
    @Throws(Exception::class)
    fun middlewareCanFlattenList() {
        // Flatten a list into key-value pairs
        val keyToFlatten = "flatten"
        val analytics = builder
            .useDestinationMiddleware(
                "foo"
            ) { chain -> // flatten list to key/value pair
                var payload = chain.payload()
                if (payload.type() == BasePayload.Type.track) {
                    val track = payload as TrackPayload
                    val newProps = ValueMap()
                    newProps.putAll(track.properties())
                    if (newProps.containsKey(keyToFlatten)) {
                        val flattenList = newProps[keyToFlatten] as List<*>
                        for (i in flattenList.indices) {
                            newProps[keyToFlatten + "_" + i] = flattenList[i]
                        }
                        newProps.remove(keyToFlatten)
                        payload = track.toBuilder().properties(newProps).build()
                    }
                }
                chain.proceed(payload)
            }
            .build()
        val list = ArrayList<String>()
        list.add("val0")
        list.add("val1")
        list.add("val2")
        analytics.track("checkout started", Properties().putValue(keyToFlatten, list))

        val fooTrack = ArgumentCaptor.forClass(TrackPayload::class.java)
        verify(integrationFoo).track(fooTrack.capture())
        assertThat(fooTrack.value.properties()).containsKey("flatten_0")
        assertThat(fooTrack.value.properties()["flatten_0"]).isEqualTo("val0")
        assertThat(fooTrack.value.properties()).containsKey("flatten_1")
        assertThat(fooTrack.value.properties()["flatten_1"]).isEqualTo("val1")
        assertThat(fooTrack.value.properties()).containsKey("flatten_2")
        assertThat(fooTrack.value.properties()["flatten_2"]).isEqualTo("val2")
        assertThat(fooTrack.value.properties()).doesNotContainKey("flatten")
    }

    @Test
    fun middlewareCanDedupeIdentifyEvents() {
        // Dedupe identify events
        val dropCount = AtomicInteger(0)
        val analytics = builder
            .useDestinationMiddleware(
                "foo",
                object : Middleware {
                    var previousIdentifyPayload: IdentifyPayload? = null
                    override fun intercept(chain: Middleware.Chain) {
                        val payload = chain.payload()
                        if (payload.type() == BasePayload.Type.identify) {
                            val identifyPayload = payload as IdentifyPayload
                            if (isDeepEqual(identifyPayload, previousIdentifyPayload)) {
                                previousIdentifyPayload = identifyPayload
                                chain.proceed(payload)
                            } else {
                                dropCount.incrementAndGet()
                            }
                        }
                    }

                    private fun isDeepEqual(
                        payload: IdentifyPayload?,
                        previousPayload: IdentifyPayload?
                    ): Boolean {
                        if (payload == null && previousPayload != null ||
                            payload != null && previousPayload == null
                        ) {
                            return true
                        }
                        if (payload != null && previousPayload != null) {
                            val anonymousId = payload["anonymousId"] as String?
                            val prevAnonymousId = previousPayload["anonymousId"] as String?

                            // anonymous ID has changed
                            if (anonymousId != prevAnonymousId) {
                                return true
                            }
                            val userId = payload["userId"] as String?
                            val prevUserId = previousPayload["userId"] as String?

                            // user ID has changed
                            if (userId != prevUserId) {
                                return true
                            }

                            // traits haven't changed
                            if (payload["traits"] == previousPayload["traits"]) {
                                return false
                            }
                        }
                        return true
                    }
                }
            )
            .build()

        analytics.identify("tom")
        verify(integrationFoo, times(1)).identify(any())

        analytics.identify("tom")
        verify(integrationFoo, times(1)).identify(any())

        analytics.identify("jerry")
        verify(integrationFoo, times(2)).identify(any())

        analytics.identify(Traits().putAge(10))
        verify(integrationFoo, times(3)).identify(any())

        analytics.identify("jerry")
        verify(integrationFoo, times(3)).identify(any())

        analytics.identify("tom")
        verify(integrationFoo, times(4)).identify(any())

        assertThat(dropCount.get()).isEqualTo(2)
    }
}
