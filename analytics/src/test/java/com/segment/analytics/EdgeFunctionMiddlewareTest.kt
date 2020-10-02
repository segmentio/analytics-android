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
import com.segment.analytics.integrations.Integration
import java.lang.AssertionError
import java.lang.IllegalStateException
import java.lang.reflect.Field
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EdgeFunctionMiddlewareTest {

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

    /** Edge Function Middleware Tests **/
    @Test
    @Throws(Exception::class)
    fun edgeFunctionMiddlewareCanExist() {

        val analytics = builder
            .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
            .build()

        analytics.track("foo")

        val privateEdgeFunctions: Field = Analytics::class.java.getDeclaredField("edgeFunctionMiddleware")
        assertThat(privateEdgeFunctions).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun edgeFunctionMiddlewareOverwrites() {

        try {
            val analytics = builder
                .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
                .useSourceMiddleware(
                    Middleware { throw AssertionError("should not be invoked") }
                )
                .build()

            fail("Should not reach this state")
        } catch (exception: java.lang.Exception) {
            assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    @Throws(Exception::class)
    fun edgeFunctionDestinationMiddlewareOverwrites() {

        try {
            val analytics = builder
                .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
                .useDestinationMiddleware(
                    "test",
                    Middleware { throw AssertionError("should not be invoked") }
                )
                .build()

            fail("Should not reach this state")
        } catch (exception: java.lang.Exception) {
            assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    @Throws(Exception::class)
    fun edgeFunctionBothMiddlewareOverwrites() {

        try {
            val analytics = builder
                .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
                .useDestinationMiddleware(
                    "test",
                    Middleware { throw AssertionError("should not be invoked") }
                )
                .useSourceMiddleware(
                    Middleware { throw AssertionError("should not be invoked") }
                )
                .build()

            fail("Should not reach this state")
        } catch (exception: java.lang.Exception) {
            assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    @Throws(Exception::class)
    fun edgeFunctionNativeFirstMiddlewareException() {

        try {
            val analytics = builder
                .useDestinationMiddleware(
                    "test",
                    Middleware { throw AssertionError("should not be invoked") }
                )
                .useSourceMiddleware(
                    Middleware { throw AssertionError("should not be invoked") }
                )
                .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
                .build()

            fail("Should not reach this state")
        } catch (exception: java.lang.Exception) {
            assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        }
    }
}
