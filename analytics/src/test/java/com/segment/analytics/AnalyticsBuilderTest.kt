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
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.res.Resources
import com.nhaarman.mockitokotlin2.whenever
import com.segment.analytics.Analytics.Builder
import com.segment.analytics.Analytics.WRITE_KEY_RESOURCE_IDENTIFIER
import com.segment.analytics.core.BuildConfig
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AnalyticsBuilderTest {

    lateinit var context: Application

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = TestUtils.mockApplication()
        Analytics.INSTANCES.clear()
        whenever(context.applicationContext).thenReturn(context)
    }

    @Test
    @Throws(Exception::class)
    fun invalidContextThrowsException() {
        whenever(context.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_DENIED)
        try {
            Builder(context, "foo")
            fail("Missing internet permission should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("INTERNET permission is required.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidExecutorThrowsException() {
        try {
            Builder(context, "foo").networkExecutor(null)
            fail("Null executor should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("Executor service must not be null.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidSourceMiddlewareThrowsException() {
        try {
            Builder(context, "foo").useSourceMiddleware(null)
            fail("Null middleware should throw exception.")
        } catch (expected: NullPointerException) {
            assertThat(expected).hasMessage("middleware == null")
        }

        try {
            val middleware = Middleware { throw AssertionError("should not be invoked") }
            Builder(context, "foo").useSourceMiddleware(middleware).useSourceMiddleware(middleware)
            fail("Registering middleware twice throw exception.")
        } catch (expected: IllegalStateException) {
            assertThat(expected).hasMessage("Source Middleware is already registered.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidDestinationMiddlewareThrowsException() {
        try {
            Builder(context, "foo").useDestinationMiddleware(null, null)
            fail("Null key should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("key must not be null or empty.")
        }

        try {
            Builder(context, "foo").useDestinationMiddleware("", null)
            fail("Null key should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("key must not be null or empty.")
        }

        try {
            Builder(context, "foo").useDestinationMiddleware("foo", null)
            fail("Null middleware should throw exception.")
        } catch (expected: NullPointerException) {
            assertThat(expected).hasMessage("middleware == null")
        }

        try {
            val middleware = Middleware { throw AssertionError("should not be invoked") }
            Builder(context, "foo")
                .useDestinationMiddleware("bar", middleware)
                .useDestinationMiddleware("bar", middleware)
            fail("Registering middleware twice throw exception.")
        } catch (expected: IllegalStateException) {
            assertThat(expected).hasMessage("Destination Middleware is already registered.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidWriteKeyThrowsException() {
        try {
            Builder(context, "")
            fail("Blank writeKey should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("writeKey must not be empty.")
        }

        try {
            Builder(context, "    ")
            fail("Blank writeKey should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("writeKey must not be empty.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidWriteKeyFromResourcesThrowsException() {
        mockWriteKeyInResources(context, "")
        try {
            Analytics.with(context)
            fail("Empty writeKey should throw exception.")
        } catch (expected: java.lang.IllegalArgumentException) {
            assertThat(expected).hasMessage("writeKey must not be empty.")
        }

        mockWriteKeyInResources(context, "    ")
        try {
            Analytics.with(context)
            fail("Blank writeKey should throw exception.")
        } catch (expected: java.lang.IllegalArgumentException) {
            assertThat(expected).hasMessage("writeKey must not be empty.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidQueueSizeThrowsException() {
        try {
            Builder(context, "foo").flushQueueSize(-1)
            fail("flushQueueSize < 0 should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("flushQueueSize must be greater than or equal to zero.")
        }

        try {
            Builder(context, "foo").flushQueueSize(0)
            fail("flushQueueSize = 0 should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("flushQueueSize must be greater than or equal to zero.")
        }

        try {
            Builder(context, "foo").flushQueueSize(251)
            fail("flushQueueSize = 251 should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("flushQueueSize must be less than or equal to 250.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidFlushIntervalThrowsException() {
        try {
            Builder(context, "foo").flushInterval(-1, TimeUnit.DAYS)
            fail("flushInterval < 0 should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("flushInterval must be greater than zero.")
        }

        try {
            Builder(context, "foo").flushInterval(1, null)
            fail("null unit should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("timeUnit must not be null.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidOptionsThrowsException() {
        try {
            Builder(context, "foo").defaultOptions(null)
            fail("null options should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("defaultOptions must not be null.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidTagThrowsException() {
        try {
            Builder(context, "foo").tag(null)
            fail("Null tag should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("tag must not be null or empty.")
        }

        try {
            Builder(context, "foo").tag("")
            fail("Empty tag should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("tag must not be null or empty.")
        }

        try {
            Builder(context, "foo").tag("    ")
            fail("Blank tag should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("tag must not be null or empty.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidLogLevelThrowsException() {
        try {
            Builder(context, "foo").logLevel(null)
            fail("Setting null LogLevel should throw exception.")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("LogLevel must not be null.")
        }
    }

    private fun mockWriteKeyInResources(context: Context, writeKey: String) {
        val resources = Mockito.mock(Resources::class.java)
        whenever(context.resources).thenReturn(resources)
        whenever(resources.getIdentifier(eq(WRITE_KEY_RESOURCE_IDENTIFIER), eq("string"), any()))
            .thenReturn(1)
        whenever(resources.getString(1)).thenReturn(writeKey)
    }

    @Test
    fun invalidDefaultProjectSettingsThrowsException() {
        try {
            Builder(context, "foo").defaultProjectSettings(null)
            fail("Null defaultProjectSettings should throw exception.")
        } catch (expected: NullPointerException) {
            assertThat(expected).hasMessage("defaultProjectSettings == null")
        }
    }
}
