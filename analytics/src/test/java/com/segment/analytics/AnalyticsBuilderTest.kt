package com.segment.analytics

import android.Manifest.permission.INTERNET
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.res.Resources
import com.nhaarman.mockitokotlin2.whenever
import com.segment.analytics.Analytics.Builder
import com.segment.analytics.Analytics.WRITE_KEY_RESOURCE_IDENTIFIER
import com.segment.analytics.core.*
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = [18], manifest = Config.NONE)
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
        try {
            Builder(null, null)
            Assertions.fail("Null context should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("Context must not be null.")
        }

        whenever(context.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_DENIED)
        try {
            Builder(context, "foo")
            Assertions.fail("Missing internet permission should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("INTERNET permission is required.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidExecutorThrowsException() {
        try {
            Builder(context, "foo").networkExecutor(null)
            Assertions.fail("Null executor should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("Executor service must not be null.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidSourceMiddlewareThrowsException() {
        try {
            Builder(context, "foo").useSourceMiddleware(null)
            Assertions.fail("Null middleware should throw exception.")
        } catch (expected: NullPointerException) {
            Assertions.assertThat(expected).hasMessage("middleware == null")
        }

        try {
            val middleware = Middleware { throw AssertionError("should not be invoked") }
            Builder(context, "foo").useSourceMiddleware(middleware).useSourceMiddleware(middleware)
            Assertions.fail("Registering middleware twice throw exception.")
        } catch (expected: IllegalStateException) {
            Assertions.assertThat(expected).hasMessage("Source Middleware is already registered.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidDestinationMiddlewareThrowsException() {
        try {
            Builder(context, "foo").useDestinationMiddleware(null, null)
            Assertions.fail("Null key should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("key must not be null or empty.")
        }

        try {
            Builder(context, "foo").useDestinationMiddleware("", null)
            Assertions.fail("Null key should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("key must not be null or empty.")
        }

        try {
            Builder(context, "foo").useDestinationMiddleware("foo", null)
            Assertions.fail("Null middleware should throw exception.")
        } catch (expected: NullPointerException) {
            Assertions.assertThat(expected).hasMessage("middleware == null")
        }

        try {
            val middleware = Middleware { throw AssertionError("should not be invoked") }
            Builder(context, "foo")
                    .useDestinationMiddleware("bar", middleware)
                    .useDestinationMiddleware("bar", middleware)
            Assertions.fail("Registering middleware twice throw exception.")
        } catch (expected: IllegalStateException) {
            Assertions.assertThat(expected).hasMessage("Destination Middleware is already registered.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidWriteKeyThrowsException() {
        try {
            Builder(context, null)
            Assertions.fail("Null writeKey should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("writeKey must not be null or empty.")
        }

        try {
            Builder(context, "")
            Assertions.fail("Blank writeKey should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("writeKey must not be null or empty.")
        }

        try {
            Builder(context, "    ")
            Assertions.fail("Blank writeKey should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("writeKey must not be null or empty.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidWriteKeyFromResourcesThrowsException() {
        mockWriteKeyInResources(context, null)
        try {
            Analytics.with(context)
            Assertions.fail("Null writeKey should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("writeKey must not be null or empty.")
        }

        mockWriteKeyInResources(context, "")
        try {
            Analytics.with(context)
            Assertions.fail("Empty writeKey should throw exception.")
        } catch (expected: java.lang.IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("writeKey must not be null or empty.")
        }

        mockWriteKeyInResources(context, "    ")
        try {
            Analytics.with(context)
            Assertions.fail("Blank writeKey should throw exception.")
        } catch (expected: java.lang.IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("writeKey must not be null or empty.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidQueueSizeThrowsException() {
        try {
            Builder(context, "foo").flushQueueSize(-1)
            Assertions.fail("flushQueueSize < 0 should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("flushQueueSize must be greater than or equal to zero.")
        }

        try {
            Builder(context, "foo").flushQueueSize(0)
            Assertions.fail("flushQueueSize = 0 should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("flushQueueSize must be greater than or equal to zero.")
        }

        try {
            Builder(context, "foo").flushQueueSize(251)
            Assertions.fail("flushQueueSize = 251 should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("flushQueueSize must be less than or equal to 250.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidFlushIntervalThrowsException() {
        try {
            Builder(context, "foo").flushInterval(-1, TimeUnit.DAYS)
            Assertions.fail("flushInterval < 0 should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("flushInterval must be greater than zero.")
        }

        try {
            Builder(context, "foo").flushInterval(1, null)
            Assertions.fail("null unit should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("timeUnit must not be null.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidOptionsThrowsException() {
        try {
            Builder(context, "foo").defaultOptions(null)
            Assertions.fail("null options should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("defaultOptions must not be null.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidTagThrowsException() {
        try {
            Builder(context, "foo").tag(null)
            Assertions.fail("Null tag should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("tag must not be null or empty.")
        }

        try {
            Builder(context, "foo").tag("")
            Assertions.fail("Empty tag should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("tag must not be null or empty.")
        }

        try {
            Builder(context, "foo").tag("    ")
            Assertions.fail("Blank tag should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("tag must not be null or empty.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun invalidLogLevelThrowsException() {
        try {
            Builder(context, "foo").logLevel(null)
            Assertions.fail("Setting null LogLevel should throw exception.")
        } catch (expected: IllegalArgumentException) {
            Assertions.assertThat(expected).hasMessage("LogLevel must not be null.")
        }
    }

    private fun mockWriteKeyInResources(context: Context, writeKey: String?) {
        val resources = Mockito.mock(Resources::class.java)
        whenever(context.resources).thenReturn(resources)
        whenever(resources.getIdentifier(eq(WRITE_KEY_RESOURCE_IDENTIFIER), eq("string"), eq("string")))
                .thenReturn(1)
        whenever(resources.getString(1)).thenReturn(writeKey)
    }

    @Test
    fun invalidDefaultProjectSettingsThrowsException() {
        try {
            Builder(context, "foo").defaultProjectSettings(null)
            Assertions.fail("Null defaultProjectSettings should throw exception.")
        } catch (expected: NullPointerException) {
            Assertions.assertThat(expected).hasMessage("defaultProjectSettings == null")
        }
    }
}