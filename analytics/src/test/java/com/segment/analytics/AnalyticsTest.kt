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

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Nullable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.segment.analytics.ProjectSettings.create
import com.segment.analytics.TestUtils.NoDescriptionMatcher
import com.segment.analytics.TestUtils.grantPermission
import com.segment.analytics.TestUtils.mockApplication
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.Integration
import com.segment.analytics.integrations.Logger
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload
import com.segment.analytics.internal.Utils.AnalyticsNetworkExecutorService
import com.segment.analytics.internal.Utils.DEFAULT_API_HOST
import com.segment.analytics.internal.Utils.DEFAULT_FLUSH_INTERVAL
import com.segment.analytics.internal.Utils.DEFAULT_FLUSH_QUEUE_SIZE
import com.segment.analytics.internal.Utils.isNullOrEmpty
import java.io.IOException
import java.lang.Boolean.TRUE
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.MapEntry
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations.initMocks
import org.mockito.Spy
import org.mockito.hamcrest.MockitoHamcrest.argThat
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@Ignore("failing test. need to fix")
open class AnalyticsTest {
    private val SETTINGS =
        """
            |{
            |  "integrations": {
            |    "test": { 
            |      "foo": "bar"
            |    },
            |    "Segment.io": {
            |    }
            |  },
            | "plan": {
            |
            |  }
            |}
            """.trimMargin()

    @Mock
    private lateinit var traitsCache: Traits.Cache
    @Spy
    private lateinit var networkExecutor: AnalyticsNetworkExecutorService

    @Spy
    private var analyticsExecutor: ExecutorService = TestUtils.SynchronousExecutor()

    @Mock
    private lateinit var client: Client

    @Mock
    private lateinit var stats: Stats

    @Mock
    private lateinit var projectSettingsCache: ProjectSettings.Cache

    @Mock
    private lateinit var integration: Integration<*>

    @Mock
    lateinit var lifecycle: Lifecycle
    private lateinit var defaultOptions: Options
    private lateinit var factory: Integration.Factory
    private lateinit var optOut: BooleanPreference
    private lateinit var application: Application
    private lateinit var traits: Traits
    private lateinit var analyticsContext: AnalyticsContext
    private lateinit var analytics: Analytics
    @Mock
    private lateinit var jsMiddleware: JSMiddleware

    @Before
    @Throws(IOException::class, NameNotFoundException::class)
    fun setUp() {
        Analytics.INSTANCES.clear()

        initMocks(this)
        defaultOptions = Options()
        application = mockApplication()
        traits = Traits.create()
        whenever(traitsCache.get()).thenReturn(traits)

        val packageInfo = PackageInfo()
        packageInfo.versionCode = 100
        packageInfo.versionName = "1.0.0"

        val packageManager = Mockito.mock(PackageManager::class.java)
        whenever(packageManager.getPackageInfo("com.foo", 0)).thenReturn(packageInfo)
        whenever(application.packageName).thenReturn("com.foo")
        whenever(application.packageManager).thenReturn(packageManager)

        analyticsContext = Utils.createContext(traits)
        factory = object : Integration.Factory {
            override fun create(settings: ValueMap, analytics: Analytics): Integration<*>? {
                return integration
            }

            override fun key(): String {
                return "test"
            }
        }
        whenever(projectSettingsCache.get())
            .thenReturn(create(Cartographer.INSTANCE.fromJson(SETTINGS)))

        val sharedPreferences =
            RuntimeEnvironment.application
                .getSharedPreferences("analytics-test-qaz", MODE_PRIVATE)
        optOut = BooleanPreference(sharedPreferences, "opt-out-test", false)

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.VERBOSE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            false,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        // Used by singleton tests.
        grantPermission(RuntimeEnvironment.application, android.Manifest.permission.INTERNET)
    }

    @After
    fun tearDown() {
        RuntimeEnvironment.application
            .getSharedPreferences("analytics-android-qaz", MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun invalidIdentity() {
        try {
            analytics.identify(null, null, null)
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessage("Either userId or some traits must be provided.")
        }
    }

    @Test
    fun identify() {
        analytics.identify("prateek", Traits().putUsername("f2prateek"), null)

        verify(integration)
            .identify(
                argThat<IdentifyPayload>(
                    object : TestUtils.NoDescriptionMatcher<IdentifyPayload>() {
                        override fun matchesSafely(item: IdentifyPayload): Boolean {
                            return item.userId() == "prateek" &&
                                item.traits().username() == "f2prateek"
                        }
                    })
            )
    }

    @Test
    fun identifyUpdatesCache() {
        analytics.identify("foo", Traits().putValue("bar", "qaz"), null)

        assertThat(traits).contains(MapEntry.entry("userId", "foo"))
        assertThat(traits).contains(MapEntry.entry("bar", "qaz"))
        assertThat(analyticsContext.traits()).contains(MapEntry.entry("userId", "foo"))
        assertThat(analyticsContext.traits()).contains(MapEntry.entry("bar", "qaz"))
        verify(traitsCache).set(traits)
        verify(integration)
            .identify(
                argThat<IdentifyPayload>(
                    object : NoDescriptionMatcher<IdentifyPayload>() {
                        override fun matchesSafely(item: IdentifyPayload): Boolean {
                            // Exercises a bug where payloads didn't pick up userId in identify correctly.
                            // https://github.com/segmentio/analytics-android/issues/169
                            return item.userId() == "foo"
                        }
                    })
            )
    }

    @Test
    fun identifyNullTraits() {
        analytics.identify("userId", null, null)

        assertThat(traits.userId()).isEqualTo("userId")
        assertThat(traits.username()).isNull()
    }

    @Test
    fun identifySavesPreviousTraits() {
        analytics.identify("userId", Traits().putUsername("username"), null)
        analytics.identify("userId")

        assertThat(traits.userId()).isEqualTo("userId")
        assertThat(traits.username()).isEqualTo("username")
    }

    @Test
    @Nullable
    fun invalidGroup() {
        try {
            analytics.group("")
            fail("empty groupId and name should throw exception")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("groupId must not be null or empty.")
        }
    }

    @Test
    fun group() {
        analytics.group("segment", Traits().putEmployees(42), null)

        verify(integration)
            .group(
                argThat<GroupPayload>(
                    object : NoDescriptionMatcher<GroupPayload>() {
                        override fun matchesSafely(item: GroupPayload): Boolean {
                            return item.groupId() == "segment" &&
                                item.traits().employees() == 42L
                        }
                    })
            )
    }

    @Test
    fun invalidTrack() {
        try {
            analytics.track(null.toString())
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessage("event must not be null or empty.")
        }
        try {
            analytics.track("   ")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessage("event must not be null or empty.")
        }
    }

    @Test
    fun track() {
        analytics.track("wrote tests", Properties().putUrl("github.com"))
        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "wrote tests" &&
                                payload.properties().url() == "github.com"
                        }
                    })
            )
    }

    @Test
    @Throws(IOException::class)
    fun invalidScreen() {
        try {
            analytics.screen(null, null as String?)
            fail("null category and name should throw exception")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("either category or name must be provided.")
        }

        try {
            analytics.screen("", "")
            fail("empty category and name should throw exception")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("either category or name must be provided.")
        }
    }

    @Test
    fun screen() {
        analytics.screen("android", "saw tests", Properties().putUrl("github.com"))
        verify(integration)
            .screen(
                argThat<ScreenPayload>(
                    object : NoDescriptionMatcher<ScreenPayload>() {
                        override fun matchesSafely(payload: ScreenPayload): Boolean {
                            return payload.name() == "saw tests" &&
                                payload.category() == "android" &&
                                payload.properties().url() == "github.com"
                        }
                    })
            )
    }

    @Test
    fun optionsDisableIntegrations() {
        analytics.screen("foo", "bar", null, Options().setIntegration("test", false))
        analytics.track("foo", null, Options().setIntegration("test", false))
        analytics.group("foo", null, Options().setIntegration("test", false))
        analytics.identify("foo", null, Options().setIntegration("test", false))
        analytics.alias("foo", Options().setIntegration("test", false))

        analytics.screen(
            "foo", "bar", null, Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false)
        )
        analytics.track("foo", null, Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false))
        analytics.group("foo", null, Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false))
        analytics.identify(
            "foo", null, Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false)
        )
        analytics.alias("foo", Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false))

        verifyNoMoreInteractions(integration)
    }

    @Test
    fun optionsCustomContext() {
        analytics.track("foo", null, Options().putContext("from_tests", true))

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.context()["from_tests"] == TRUE
                        }
                    })
            )
    }

    @Test
    @Throws(IOException::class)
    fun optOutDisablesEvents() {
        analytics.optOut(true)
        analytics.track("foo")
        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(IOException::class)
    fun emptyTrackingPlan() {
        analytics.projectSettings = create(
            Cartographer.INSTANCE.fromJson(
                """
                                      |{
                                      |  "integrations": {
                                      |    "test": {
                                      |      "foo": "bar"
                                      |    }
                                      |  },
                                      |  "plan": {
                                      |  }
                                      |}
                                      """.trimMargin()
            )
        )

        analytics.track("foo")
        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "foo"
                        }
                    })
            )
        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(IOException::class)
    fun emptyEventPlan() {
        analytics.projectSettings = create(
            Cartographer.INSTANCE.fromJson(
                """
                              |{
                              |  "integrations": {
                              |    "test": {
                              |      "foo": "bar"
                              |    }
                              |  },
                              |  "plan": {
                              |    "track": {
                              |    }
                              |  }
                              |}
                              """.trimMargin()
            )
        )
        analytics.track("foo")
        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "foo"
                        }
                    })
            )
        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(IOException::class)
    fun trackingPlanDisablesEvent() {
        analytics.projectSettings = create(
            Cartographer.INSTANCE.fromJson(
                """
                              |{
                              |  "integrations": {
                              |    "test": {
                              |      "foo": "bar"
                              |    }
                              |  },
                              |  "plan": {
                              |    "track": {
                              |      "foo": {
                              |        "enabled": false
                              |      }
                              |    }
                              |  }
                              |}
                              """.trimMargin()
            )
        )
        analytics.track("foo")
        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(IOException::class)
    fun trackingPlanDisablesEventForSingleIntegration() {
        analytics.projectSettings = create(
            Cartographer.INSTANCE.fromJson(
                """
                              |{
                              |  "integrations": {
                              |    "test": {
                              |      "foo": "bar"
                              |    }
                              |  },
                              |  "plan": {
                              |    "track": {
                              |      "foo": {
                              |        "enabled": true,
                              |        "integrations": {
                              |          "test": false
                              |        }
                              |      }
                              |    }
                              |  }
                              |}
                              """.trimMargin()
            )
        )
        analytics.track("foo")
        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(IOException::class)
    fun trackingPlanDisabledEventCannotBeOverriddenByOptions() {
        analytics.projectSettings = create(
            Cartographer.INSTANCE.fromJson(
                """
                              |{
                              |  "integrations": {
                              |    "test": {
                              |      "foo": "bar"
                              |    }
                              |  },
                              |  "plan": {
                              |    "track": {
                              |      "foo": {
                              |        "enabled": false
                              |      }
                              |    }
                              |  }
                              |}
                              """.trimMargin()
            )
        )
        analytics.track("foo", null, Options().setIntegration("test", true))
        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(IOException::class)
    fun trackingPlanDisabledEventForIntegrationOverriddenByOptions() {
        analytics.projectSettings = create(
            Cartographer.INSTANCE.fromJson(
                """
                              |{
                              |  "integrations": {
                              |    "test": {
                              |      "foo": "bar"
                              |    }
                              |  },
                              |  "plan": {
                              |    "track": {
                              |      "foo": {
                              |        "enabled": true,
                              |        "integrations": {
                              |          "test": false
                              |        }
                              |      }
                              |    }
                              |  }
                              |}
                              """.trimMargin()
            )
        )
        analytics.track("foo", null, Options().setIntegration("test", true))
        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "foo"
                        }
                    })
            )
        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(IOException::class)
    fun invalidAlias() {
        try {
            analytics.alias("")
            fail("empty new id should throw error")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("newId must not be null or empty.")
        }
    }

    @Test
    fun alias() {
        val anonymousId = traits.anonymousId()
        analytics.alias("foo")
        val payloadArgumentCaptor =
            ArgumentCaptor.forClass(AliasPayload::class.java)
        verify(integration).alias(payloadArgumentCaptor.capture())
        assertThat(payloadArgumentCaptor.value).containsEntry("previousId", anonymousId)
        assertThat(payloadArgumentCaptor.value).containsEntry("userId", "foo")
    }

    @Test
    fun aliasWithCachedUserID() {
        analytics.identify(
            "prayansh", Traits().putValue("bar", "qaz"), null
        ) // refer identifyUpdatesCache
        analytics.alias("foo")
        val payloadArgumentCaptor =
            ArgumentCaptor.forClass(AliasPayload::class.java)
        verify(integration).alias(payloadArgumentCaptor.capture())
        assertThat(payloadArgumentCaptor.value).containsEntry("previousId", "prayansh")
        assertThat(payloadArgumentCaptor.value).containsEntry("userId", "foo")
    }

    @Test
    fun flush() {
        analytics.flush()

        verify(integration).flush()
    }

    @Test
    fun reset() {
        analytics.reset()

        verify(integration).reset()
    }

    @Test
    @Throws(Exception::class)
    fun getSnapshot() {
        analytics.snapshot

        verify(stats).createSnapshot()
    }

    @Test
    fun logoutClearsTraitsAndUpdatesContext() {
        analyticsContext.setTraits(Traits().putAge(20).putAvatar("bar"))

        analytics.logout()

        verify(traitsCache).delete()
        verify(traitsCache)
            .set(
                argThat(
                    object : TypeSafeMatcher<Traits>() {
                        override fun matchesSafely(traits: Traits): Boolean {
                            return !isNullOrEmpty(traits.anonymousId())
                        }

                        override fun describeTo(description: Description) {}
                    })
            )
        assertThat(analyticsContext.traits()).hasSize(1)
        assertThat(analyticsContext.traits()).containsKey("anonymousId")
    }

    @Test
    fun onIntegrationReadyShouldFailForNullKey() {
        try {
            analytics.onIntegrationReady(null as String?, Mockito.mock(Analytics.Callback::class.java))
            fail("registering for null integration should fail")
        } catch (e: java.lang.IllegalArgumentException) {
            assertThat(e).hasMessage("key cannot be null or empty.")
        }
    }

    @Test
    fun onIntegrationReady() {
        val callback: Analytics.Callback<*> = Mockito.mock(Analytics.Callback::class.java)
        analytics.onIntegrationReady("test", callback)
        verify(callback).onReady(null)
    }

    @Test
    fun shutdown() {
        assertThat(analytics.shutdown).isFalse()
        analytics.shutdown()
        verify(application).unregisterActivityLifecycleCallbacks(analytics.activityLifecycleCallback)
        verify(stats).shutdown()
        verify(networkExecutor).shutdown()
        assertThat(analytics.shutdown).isTrue()
        try {
            analytics.track("foo")
            fail("Enqueuing a message after shutdown should throw.")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessage("Cannot enqueue messages after client is shutdown.")
        }

        try {
            analytics.flush()
            fail("Enqueuing a message after shutdown should throw.")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessage("Cannot enqueue messages after client is shutdown.")
        }
    }

    @Test
    fun shutdownTwice() {
        assertThat(analytics.shutdown).isFalse()
        analytics.shutdown()
        analytics.shutdown()
        verify(stats).shutdown()
        assertThat(analytics.shutdown).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun shutdownDisallowedOnCustomSingletonInstance() {
        Analytics.singleton = null
        try {
            val analytics = Analytics.Builder(RuntimeEnvironment.application, "foo").build()
            Analytics.setSingletonInstance(analytics)
            analytics.shutdown()
            fail("Calling shutdown() on static singleton instance should throw")
        } catch (ignored: UnsupportedOperationException) {
        }
    }

    @Test
    fun setSingletonInstanceMayOnlyBeCalledOnce() {
        Analytics.singleton = null

        val analytics = Analytics.Builder(RuntimeEnvironment.application, "foo").build()
        Analytics.setSingletonInstance(analytics)

        try {
            Analytics.setSingletonInstance(analytics)
            fail("Can't set singleton instance twice.")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessage("Singleton instance already exists.")
        }
    }

    @Test
    fun setSingletonInstanceAfterWithFails() {
        Analytics.singleton = null
        Analytics.setSingletonInstance(Analytics.Builder(RuntimeEnvironment.application, "foo").build())

        val analytics = Analytics.Builder(RuntimeEnvironment.application, "bar").build()
        try {
            Analytics.setSingletonInstance(analytics)
            fail("Can't set singleton instance after with().")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessage("Singleton instance already exists.")
        }
    }

    @Test
    fun setSingleInstanceReturnedFromWith() {
        Analytics.singleton = null
        val analytics = Analytics.Builder(RuntimeEnvironment.application, "foo").build()
        Analytics.setSingletonInstance(analytics)
        assertThat(Analytics.with(RuntimeEnvironment.application)).isSameAs(analytics)
    }

    @Test
    @Throws(Exception::class)
    fun multipleInstancesWithSameTagThrows() {
        Analytics.Builder(RuntimeEnvironment.application, "foo").build()
        try {
            Analytics.Builder(RuntimeEnvironment.application, "bar").tag("foo").build()
            fail("Creating client with duplicate should throw.")
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageContaining("Duplicate analytics client created with tag: foo.")
        }
    }

    @Test
    @Throws(Exception::class)
    fun multipleInstancesWithSameTagIsAllowedAfterShutdown() {
        Analytics.Builder(RuntimeEnvironment.application, "foo").build().shutdown()
        Analytics.Builder(RuntimeEnvironment.application, "bar").tag("foo").build()
    }

    @Test
    @Throws(Exception::class)
    fun getSnapshotInvokesStats() {
        analytics.snapshot
        verify(stats).createSnapshot()
    }

    @Test
    @Throws(Exception::class)
    fun invalidURlsThrowAndNotCrash() {
        val connection = ConnectionFactory()

        try {
            connection.openConnection("SOME_BUSTED_URL")
            fail("openConnection did not throw when supplied an invalid URL as expected.")
        } catch (expected: IOException) {
            assertThat(expected).hasMessageContaining("Attempted to use malformed url")
            assertThat(expected).isInstanceOf(IOException::class.java)
        }
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsInstalled() {
        Analytics.INSTANCES.clear()
        val callback = AtomicReference<DefaultLifecycleObserver>()
        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        callback.get().onCreate(mockLifecycleOwner)

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() ==
                                "Application Installed" &&
                                payload.properties()
                                .getString("version") == "1.0.0" &&
                                payload.properties()
                                .getString("build") == 100.toString()
                        }
                    })
            )

        callback.get().onCreate(mockLifecycleOwner)
        verifyNoMoreInteractions(integration) // Application Installed is not duplicated
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsUpdated() {
        Analytics.INSTANCES.clear()

        val packageInfo = PackageInfo()
        packageInfo.versionCode = 101
        packageInfo.versionName = "1.0.1"

        val sharedPreferences =
            RuntimeEnvironment.application.getSharedPreferences("analytics-android-qaz", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("build", 100)
        editor.putString("version", "1.0.0")
        editor.apply()
        whenever(application.getSharedPreferences("analytics-android-qaz", MODE_PRIVATE))
            .thenReturn(sharedPreferences)

        val packageManager = Mockito.mock(PackageManager::class.java)
        whenever(packageManager.getPackageInfo("com.foo", 0)).thenReturn(packageInfo)
        whenever(application.packageName).thenReturn("com.foo")
        whenever(application.packageManager).thenReturn(packageManager)

        val callback = AtomicReference<DefaultLifecycleObserver>()
        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        callback.get().onCreate(mockLifecycleOwner)

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() ==
                                "Application Updated" &&
                                payload.properties()
                                .getString("previous_version") == "1.0.0" &&
                                payload.properties()
                                .getString("previous_build") == 100.toString() &&
                                payload.properties()
                                .getString("version") == "1.0.1" &&
                                payload.properties()
                                .getString("build") == 101.toString()
                        }
                    })
            )
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun recordScreenViews() {
        Analytics.INSTANCES.clear()

        val callback = AtomicReference<ActivityLifecycleCallbacks>()
        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            false,
            CountDownLatch(0),
            true,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val activity = Mockito.mock(Activity::class.java)
        val packageManager = Mockito.mock(PackageManager::class.java)
        val info = Mockito.mock(ActivityInfo::class.java)

        whenever(activity.packageManager).thenReturn(packageManager)
        //noinspection WrongConstant
        whenever(packageManager.getActivityInfo(any(ComponentName::class.java), eq(PackageManager.GET_META_DATA)))
            .thenReturn(info)
        whenever(info.loadLabel(packageManager)).thenReturn("Foo")

        callback.get().onActivityStarted(activity)

        analytics.screen("Foo")
        verify(integration)
            .screen(
                argThat<ScreenPayload>(
                    object : NoDescriptionMatcher<ScreenPayload>() {
                        override fun matchesSafely(payload: ScreenPayload): Boolean {
                            return payload.name() == "Foo"
                        }
                    })
            )
    }

    @Config(sdk = [22])
    @Test
    fun trackDeepLinks() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()
        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            true,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val expectedURL = "app://track.com/open?utm_id=12345&gclid=abcd&nope="
        val referrerUrl = "android-app:/com.package.app"

        val activity = Mockito.mock(Activity::class.java)
        val intent = Mockito.mock(Intent::class.java)
        val uri = Uri.parse(expectedURL)
        val referrer = Uri.parse(referrerUrl)

        whenever(activity.referrer).thenReturn(referrer)
        whenever(intent.data).thenReturn(uri)
        whenever(activity.intent).thenReturn(intent)

        callback.get().onActivityCreated(activity, Bundle())

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Deep Link Opened" &&
                                payload.properties().getString("url") == expectedURL &&
                                payload.properties().getString("gclid") == "abcd" &&
                                payload.properties().getString("utm_id") == "12345" &&
                                payload.properties().getString("referrer") == referrerUrl
                        }
                    })
            )
    }

    @Config(sdk = [18])
    @Test
    fun trackDeepLinks_api18() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()
        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            true,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val expectedURL = "app://track.com/open?utm_id=12345&gclid=abcd&nope="
        val referrerUrl = "android-app:/com.package.app"

        val activity = Mockito.mock(Activity::class.java)
        val intent = Mockito.mock(Intent::class.java)
        val uri = Uri.parse(expectedURL)
        val referrer = Uri.parse(referrerUrl)

        whenever(intent.getParcelableExtra<Uri>(Intent.EXTRA_REFERRER)).thenReturn(referrer)
        whenever(intent.data).thenReturn(uri)
        whenever(activity.intent).thenReturn(intent)

        callback.get().onActivityCreated(activity, Bundle())

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Deep Link Opened" &&
                                payload.properties().getString("url") == expectedURL &&
                                payload.properties().getString("gclid") == "abcd" &&
                                payload.properties().getString("utm_id") == "12345" &&
                                payload.properties().getString("referrer") == referrerUrl
                        }
                    })
            )
    }

    @Test
    fun trackDeepLinks_disabled() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val expectedURL = "app://track.com/open?utm_id=12345&gclid=abcd&nope="

        val activity = Mockito.mock(Activity::class.java)
        val intent = Mockito.mock(Intent::class.java)
        val uri = Uri.parse(expectedURL)

        whenever(intent.data).thenReturn(uri)
        whenever(activity.intent).thenReturn(intent)

        verify(integration, Mockito.never())
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Deep Link Opened" &&
                                payload.properties()
                                .getString("url") == expectedURL &&
                                payload.properties()
                                .getString("gclid") == "abcd" &&
                                payload.properties()
                                .getString("utm_id") == "12345"
                        }
                    })
            )
    }

    @Test
    fun trackDeepLinks_bad_uri() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val expectedURL = "wc:foo-bar-jk@1bridge=https%3A%2F%2Fbridge.walletconnect.org&key=1234"

        val activity = Mockito.mock(Activity::class.java)
        val intent = Mockito.mock(Intent::class.java)
        val uri = Uri.parse(expectedURL)

        whenever(intent.data).thenReturn(uri)
        whenever(activity.intent).thenReturn(intent)

        verify(integration, Mockito.never())
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Deep Link Opened" &&
                                payload.properties()
                                .getString("url") == expectedURL
                        }
                    })
            )
    }

    @Test
    fun trackDeepLinks_null() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val activity = Mockito.mock(Activity::class.java)

        whenever(activity.intent).thenReturn(null)

        callback.get().onActivityCreated(activity, Bundle())

        verify(integration, Mockito.never())
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Deep Link Opened"
                        }
                    })
            )
    }

    @Test
    fun trackDeepLinks_nullData() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val activity = Mockito.mock(Activity::class.java)

        val intent = Mockito.mock(Intent::class.java)

        whenever(activity.intent).thenReturn(intent)
        whenever(intent.data).thenReturn(null)

        callback.get().onActivityCreated(activity, Bundle())

        verify(integration, Mockito.never())
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Deep Link Opened"
                        }
                    })
            )
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun registerActivityLifecycleCallbacks() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val activity = Mockito.mock(Activity::class.java)
        val bundle = Bundle()

        callback.get().onActivityCreated(activity, bundle)
        verify(integration).onActivityCreated(activity, bundle)

        callback.get().onActivityStarted(activity)
        verify(integration).onActivityStarted(activity)

        callback.get().onActivityResumed(activity)
        verify(integration).onActivityResumed(activity)

        callback.get().onActivityPaused(activity)
        verify(integration).onActivityPaused(activity)

        callback.get().onActivityStopped(activity)
        verify(integration).onActivityStopped(activity)

        callback.get().onActivitySaveInstanceState(activity, bundle)
        verify(integration).onActivitySaveInstanceState(activity, bundle)

        callback.get().onActivityDestroyed(activity)
        verify(integration).onActivityDestroyed(activity)

        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsApplicationOpened() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<DefaultLifecycleObserver>()

        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        callback.get().onCreate(mockLifecycleOwner)
        callback.get().onStart(mockLifecycleOwner)

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Application Opened" &&
                                payload.properties().getString("version") == "1.0.0" &&
                                payload.properties().getString("build") == 100.toString() &&
                                !payload.properties().getBoolean("from_background", true)
                        }
                    })
            )
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsApplicationBackgrounded() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<DefaultLifecycleObserver>()

        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        val backgroundedActivity = Mockito.mock(Activity::class.java)
        whenever(backgroundedActivity.isChangingConfigurations).thenReturn(false)

        callback.get().onCreate(mockLifecycleOwner)
        callback.get().onStart(mockLifecycleOwner)
        callback.get().onResume(mockLifecycleOwner)
        callback.get().onStop(mockLifecycleOwner)

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Application Backgrounded"
                        }
                    })
            )
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsApplicationForegrounded() {
        Analytics.INSTANCES.clear()

        val callback =
            AtomicReference<DefaultLifecycleObserver>()

        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        callback.get().onCreate(mockLifecycleOwner)
        callback.get().onStart(mockLifecycleOwner)
        callback.get().onStop(mockLifecycleOwner)
        callback.get().onStart(mockLifecycleOwner)

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Application Backgrounded"
                        }
                    })
            )

        verify(integration)
            .track(
                argThat<TrackPayload>(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Application Opened" &&
                                payload.properties()
                                    .getBoolean("from_background", false)
                        }
                    })
            )
    }

    @Test
    @Throws(NameNotFoundException::class)
    open fun trackApplicationLifecycleEventsApplicationOpenedOldFlow() {
        Analytics.INSTANCES.clear()
        // need to reset bcos we interact with mock in our setUp function (implicitly via analytics
        // constructor)
        Mockito.reset(lifecycle)
        val callback = AtomicReference<ActivityLifecycleCallbacks>()
        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )
        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz",
            listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(),
            emptyList(),
            emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            false,
            DEFAULT_API_HOST
        )

        val activity = Mockito.mock(Activity::class.java)
        // Verify that new methods were not registered
        verify(lifecycle, never()).addObserver(any(LifecycleObserver::class.java))
        callback.get().onActivityCreated(activity, null)
        callback.get().onActivityResumed(activity)
        verify(integration)
            .track(
                argThat(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Application Opened" &&
                                payload.properties().getString("version") == "1.0.0" &&
                                payload.properties().getString("build") == 100.toString() &&
                                !payload.properties().getBoolean("from_background", true)
                        }
                    })
            )
    }

    @Test
    @Throws(NameNotFoundException::class)
    open fun trackApplicationLifecycleEventsApplicationBackgroundedOldFlow() {
        Analytics.INSTANCES.clear()
        // need to reset bcos we interact with mock in our setUp function (implicitly via analytics
        // constructor)
        Mockito.reset(lifecycle)
        val callback = AtomicReference<ActivityLifecycleCallbacks>()
        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )
        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz",
            listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(),
            emptyList(),
            emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            false,
            DEFAULT_API_HOST
        )

        val activity = Mockito.mock(Activity::class.java)
        // Verify that new methods were not registered
        verify(lifecycle, never()).addObserver(any(LifecycleObserver::class.java))
        val backgroundedActivity: Activity = Mockito.mock(Activity::class.java)
        whenever(backgroundedActivity.isChangingConfigurations).thenReturn(false)
        callback.get().onActivityCreated(activity, null)
        callback.get().onActivityResumed(activity)
        callback.get().onActivityStopped(backgroundedActivity)
        verify(integration)
            .track(
                argThat(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Application Backgrounded"
                        }
                    })
            )
    }

    @Test
    @Throws(NameNotFoundException::class)
    open fun trackApplicationLifecycleEventsApplicationForegroundedOldFlow() {
        Analytics.INSTANCES.clear()
        // need to reset bcos we interact with mock in our setUp function (implicitly via analytics
        // constructor)
        Mockito.reset(lifecycle)
        val callback = AtomicReference<ActivityLifecycleCallbacks>()
        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )
        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz",
            listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(),
            emptyList(),
            emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            false,
            DEFAULT_API_HOST
        )

        val activity = Mockito.mock(Activity::class.java)
        // Verify that new methods were not registered
        verify(lifecycle, never()).addObserver(any(LifecycleObserver::class.java))
        val backgroundedActivity: Activity = Mockito.mock(Activity::class.java)
        whenever(backgroundedActivity.isChangingConfigurations).thenReturn(false)
        callback.get().onActivityCreated(activity, null)
        callback.get().onActivityResumed(activity)
        callback.get().onActivityStopped(backgroundedActivity)
        callback.get().onActivityResumed(activity)
        verify(integration)
            .track(
                argThat(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Application Backgrounded"
                        }
                    })
            )
        verify(integration)
            .track(
                argThat(
                    object : NoDescriptionMatcher<TrackPayload>() {
                        override fun matchesSafely(payload: TrackPayload): Boolean {
                            return payload.event() == "Application Opened" &&
                                payload.properties().getBoolean("from_background", false)
                        }
                    })
            )
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun unregisterActivityLifecycleCallbacks() {
        Analytics.INSTANCES.clear()

        val registeredCallback = AtomicReference<ActivityLifecycleCallbacks>()
        val unregisteredCallback = AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            registeredCallback.set(item)
                            return true
                        }
                    })
            )
        doNothing()
            .whenever(application)
            .unregisterActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            unregisteredCallback.set(item)
                            return true
                        }
                    })
            )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        assertThat(analytics.shutdown).isFalse()
        analytics.shutdown()

        // Same callback was registered and unregistered
        assertThat(analytics.activityLifecycleCallback).isSameAs(registeredCallback.get())
        assertThat(analytics.activityLifecycleCallback).isSameAs(unregisteredCallback.get())

        val activity = Mockito.mock(Activity::class.java)
        val bundle = Bundle()

        // Verify callbacks do not call through after shutdown
        registeredCallback.get().onActivityCreated(activity, bundle)
        verify(integration, never()).onActivityCreated(activity, bundle)

        registeredCallback.get().onActivityStarted(activity)
        verify(integration, never()).onActivityStarted(activity)

        registeredCallback.get().onActivityResumed(activity)
        verify(integration, never()).onActivityResumed(activity)

        registeredCallback.get().onActivityPaused(activity)
        verify(integration, never()).onActivityPaused(activity)

        registeredCallback.get().onActivityStopped(activity)
        verify(integration, never()).onActivityStopped(activity)

        registeredCallback.get().onActivitySaveInstanceState(activity, bundle)
        verify(integration, never()).onActivitySaveInstanceState(activity, bundle)

        registeredCallback.get().onActivityDestroyed(activity)
        verify(integration, never()).onActivityDestroyed(activity)

        verifyNoMoreInteractions(integration)
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun removeLifecycleObserver() {
        Analytics.INSTANCES.clear()

        val registeredCallback = AtomicReference<DefaultLifecycleObserver>()
        val unregisteredCallback = AtomicReference<DefaultLifecycleObserver>()

        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            registeredCallback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )
        doNothing()
            .whenever(lifecycle)
            .removeObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            unregisteredCallback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )
        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            false,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        assertThat(analytics.shutdown).isFalse()
        analytics.shutdown()
        val lifecycleObserverSpy = spy(analytics.activityLifecycleCallback)
        // Same callback was registered and unregistered
        assertThat(analytics.activityLifecycleCallback).isSameAs(registeredCallback.get())
        assertThat(analytics.activityLifecycleCallback).isSameAs(unregisteredCallback.get())

        // Verify callbacks do not call through after shutdown
        registeredCallback.get().onCreate(mockLifecycleOwner)
        verify(lifecycleObserverSpy, never()).onCreate(mockLifecycleOwner)

        registeredCallback.get().onStop(mockLifecycleOwner)
        verify(lifecycleObserverSpy, never()).onStop(mockLifecycleOwner)

        registeredCallback.get().onStart(mockLifecycleOwner)
        verify(lifecycleObserverSpy, never()).onStart(mockLifecycleOwner)

        verifyNoMoreInteractions(lifecycleObserverSpy)
    }

    @Test
    @Throws(IOException::class)
    fun loadNonEmptyDefaultProjectSettingsOnNetworkError() {
        Analytics.INSTANCES.clear()
        // Make project download empty map and thus use default settings
        whenever(projectSettingsCache.get()).thenReturn(null)
        whenever(client.fetchSettings()).thenThrow(IOException::class.java) // Simulate network error

        val defaultProjectSettings =
            ValueMap()
                .putValue(
                    "integrations",
                    ValueMap()
                        .putValue(
                            "Adjust",
                            ValueMap()
                                .putValue("appToken", "<>")
                                .putValue("trackAttributionData", true)
                        )
                )

        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            defaultProjectSettings,
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        assertThat(analytics.projectSettings).hasSize(2)
        assertThat(analytics.projectSettings).containsKey("integrations")
        assertThat(analytics.projectSettings.integrations()).hasSize(2)
        assertThat(analytics.projectSettings.integrations()).containsKey("Segment.io")
        assertThat(analytics.projectSettings.integrations()).containsKey("Adjust")
    }

    @Test
    @Throws(IOException::class)
    fun loadEmptyDefaultProjectSettingsOnNetworkError() {
        Analytics.INSTANCES.clear()
        // Make project download empty map and thus use default settings
        whenever(projectSettingsCache.get()).thenReturn(null)
        whenever(client.fetchSettings()).thenThrow(IOException::class.java) // Simulate network error

        val defaultProjectSettings = ValueMap()
        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            defaultProjectSettings,
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        assertThat(analytics.projectSettings).hasSize(2)
        assertThat(analytics.projectSettings).containsKey("integrations")
        assertThat(analytics.projectSettings.integrations()).hasSize(1)
        assertThat(analytics.projectSettings.integrations()).containsKey("Segment.io")
    }

    @Test
    @Throws(IOException::class)
    fun overwriteSegmentIoIntegration() {
        Analytics.INSTANCES.clear()
        // Make project download empty map and thus use default settings
        whenever(projectSettingsCache.get()).thenReturn(null)
        whenever(client.fetchSettings()).thenThrow(IOException::class.java) // Simulate network error

        val defaultProjectSettings = ValueMap()
            .putValue(
                "integrations",
                ValueMap()
                    .putValue(
                        "Segment.io",
                        ValueMap()
                            .putValue("appToken", "<>")
                            .putValue("trackAttributionData", true)
                    )
            )
        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            defaultProjectSettings,
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        assertThat(analytics.projectSettings).hasSize(2)
        assertThat(analytics.projectSettings).containsKey("integrations")
        assertThat(analytics.projectSettings.integrations()).containsKey("Segment.io")
        assertThat(analytics.projectSettings.integrations()).hasSize(1)
        assertThat(analytics.projectSettings.integrations().getValueMap("Segment.io"))
            .hasSize(4)
        assertThat(analytics.projectSettings.integrations().getValueMap("Segment.io"))
            .containsKey("apiKey")
        assertThat(analytics.projectSettings.integrations().getValueMap("Segment.io"))
            .containsKey("apiHost")
        assertThat(analytics.projectSettings.integrations().getValueMap("Segment.io"))
            .containsKey("appToken")
        assertThat(analytics.projectSettings.integrations().getValueMap("Segment.io"))
            .containsKey("trackAttributionData")
    }

    @Test
    fun overridingOptionsDoesNotModifyGlobalAnalytics() {
        analytics.track("event", null, Options().putContext("testProp", true))
        val payload = ArgumentCaptor.forClass(TrackPayload::class.java)
        verify(integration).track(payload.capture())
        assertThat(payload.value.context()).containsKey("testProp")
        assertThat(payload.value.context()["testProp"]).isEqualTo(true)
        assertThat(analytics.analyticsContext).doesNotContainKey("testProp")
    }

    @Test
    fun overridingOptionsWithDefaultOptionsPlusAdditional() {
        analytics.track("event", null, analytics.getDefaultOptions().putContext("testProp", true))
        val payload = ArgumentCaptor.forClass(TrackPayload::class.java)
        verify(integration).track(payload.capture())
        assertThat(payload.value.context()).containsKey("testProp")
        assertThat(payload.value.context()["testProp"]).isEqualTo(true)
        assertThat(analytics.analyticsContext).doesNotContainKey("testProp")
    }

    @Test
    fun enableExperimentalNanosecondResolutionTimestamps() {
        Analytics.INSTANCES.clear()
        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz", listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            true,
            true,
            DEFAULT_API_HOST
        )

        analytics.track("event")
        val payload = ArgumentCaptor.forClass(TrackPayload::class.java)
        verify(integration).track(payload.capture())
        val timestamp = payload.value["timestamp"] as String
        assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{9}Z")
    }

    @Test
    fun disableExperimentalNanosecondResolutionTimestamps() {
        Analytics.INSTANCES.clear()
        analytics = Analytics(
            application,
            networkExecutor,
            stats,
            traitsCache,
            analyticsContext,
            defaultOptions,
            Logger.with(Analytics.LogLevel.NONE),
            "qaz",
            listOf(factory),
            client,
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            Crypto.none(), emptyList(), emptyMap(),
            jsMiddleware,
            ValueMap(),
            lifecycle,
            false,
            true,
            DEFAULT_API_HOST
        )

        analytics.track("event")
        val payload = ArgumentCaptor.forClass(TrackPayload::class.java)
        verify(integration).track(payload.capture())
        val timestamp = payload.value["timestamp"] as String
        assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z")
    }
}
