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

import android.Manifest.permission.INTERNET
import com.google.common.util.concurrent.MoreExecutors
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload
import java.lang.AssertionError
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SourceMiddlewareTest {

    lateinit var builder: Analytics.Builder

    @Before
    fun setUp() {
        initMocks(this)
        Analytics.INSTANCES.clear()
        TestUtils.grantPermission(RuntimeEnvironment.application, INTERNET)
        builder =
            Analytics.Builder(RuntimeEnvironment.application, "write_key")
                .executor(MoreExecutors.newDirectExecutorService())
    }

    @Throws(Exception::class)
    @Test
    fun middlewareCanShortCircuit() {
        val payloadRef = AtomicReference<TrackPayload>()
        val analytics: Analytics =
            builder
                .useSourceMiddleware {
                    chain ->
                    val payload = chain.payload()
                    payloadRef.set(payload as TrackPayload)
                    chain.proceed(payload)
                }
                .useSourceMiddleware {
                    throw AssertionError("should not be invoked")
                }
                .build()

        analytics.track("foo")
        assertThat(payloadRef.get().event()).isEqualTo("foo")
    }

    @Throws(Exception::class)
    @Test
    fun middlewareCanProceed() {
        val payloadRef = AtomicReference<ScreenPayload>()
        val analytics = builder
            .useSourceMiddleware { chain -> chain.proceed(chain.payload()) }
            .useSourceMiddleware { chain ->
                val payload = chain.payload()
                payloadRef.set(payload as ScreenPayload)
                chain.proceed(payload)
            }
            .build()
        analytics.screen("foo")
        assertThat(payloadRef.get().name()).isEqualTo("foo")
    }

    @Test
    @Throws(Exception::class)
    fun middlewareCanTransform() {
        val payloadRef = AtomicReference<BasePayload>()
        val analytics = builder
            .useSourceMiddleware { chain -> chain.proceed(chain.payload().toBuilder().messageId("override").build()) }
            .useSourceMiddleware { chain ->
                val payload = chain.payload()
                payloadRef.set(payload)
                chain.proceed(payload)
            }
            .build()
        analytics.identify("prateek")
        assertThat(payloadRef.get().messageId()).isEqualTo("override")
    }
}
