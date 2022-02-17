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

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import com.google.common.collect.ImmutableMap
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import com.segment.analytics.Utils.createContext
import com.segment.analytics.core.BuildConfig
import java.util.concurrent.CountDownLatch
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.MapEntry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AnalyticsContextTest {

    lateinit var context: AnalyticsContext
    lateinit var traits: Traits

    @Before
    fun setUp() {
        traits = Traits.create()
        context = createContext(traits)
    }

    @Test
    fun create() {
        context = AnalyticsContext.create(RuntimeEnvironment.application, traits, true)
        assertThat(context)
            .containsKeys("app", "device", "library", "locale", "network", "os", "screen", "timezone", "traits")
        assertThat(context).containsEntry("userAgent", "undefined")

        assertThat(context.getValueMap("app"))
            .containsEntry("name", "org.robolectric.default")
        assertThat(context.getValueMap("app"))
            .containsEntry("version", "undefined")
        assertThat(context.getValueMap("app"))
            .containsEntry("namespace", "org.robolectric.default")
        assertThat(context.getValueMap("app"))
            .containsEntry("build", "0")

        // only check esistence of device id, since we don't know the value
        // and we can't mock static method in mockito 2
        assertThat(context.getValueMap("device"))
            .containsKey("id")
        assertThat(context.getValueMap("device"))
            .containsEntry("manufacturer", "unknown")
        assertThat(context.getValueMap("device"))
            .containsEntry("model", "unknown")
        assertThat(context.getValueMap("device"))
            .containsEntry("name", "unknown")
        assertThat(context.getValueMap("device"))
            .containsEntry("type", "android")

        assertThat(context.getValueMap("library"))
            .containsEntry("name", "analytics-android")
        assertThat(context.getValueMap("library"))
            .containsEntry("version", BuildConfig.VERSION_NAME)

        // todo: mock network state?
        assertThat(context.getValueMap("network")).isEmpty()

        assertThat(context.getValueMap("os"))
            .containsEntry("name", "Android")
        assertThat(context.getValueMap("os"))
            .containsEntry("version", "4.1.2_r1")

        assertThat(context.getValueMap("screen"))
            .containsEntry("density", 1.5f)
        assertThat(context.getValueMap("screen"))
            .containsEntry("width", 480)
        assertThat(context.getValueMap("screen"))
            .containsEntry("height", 800)
    }

    @Test
    fun createWithoutDeviceIdCollection() {
        context = AnalyticsContext.create(RuntimeEnvironment.application, traits, false)

        assertThat(context.getValueMap("device"))
            .containsEntry("id", traits.anonymousId())
        assertThat(context.getValueMap("device"))
            .containsEntry("manufacturer", "unknown")
        assertThat(context.getValueMap("device"))
            .containsEntry("model", "unknown")
        assertThat(context.getValueMap("device"))
            .containsEntry("name", "unknown")
        assertThat(context.getValueMap("device"))
            .containsEntry("type", "android")
    }

    @Test
    fun copyReturnsSameMappings() {
        val copy = context.unmodifiableCopy()
        assertThat(copy).hasSameSizeAs(context).isNotSameAs(context).isEqualTo(context)
        for ((key, value) in context) {
            assertThat(copy).contains(MapEntry.entry(key, value))
        }
    }

    @Test
    fun copyIsImmutable() {
        val copy = context.unmodifiableCopy()

        try {
            copy["foo"] = "bar"
            fail("Inserting into copy should throw UnsupportedOperationException")
        } catch (expected: UnsupportedOperationException) {
        }
    }

    @Test
    fun traitsAreCopied() {
        assertThat(context.traits()).isEqualTo(traits).isNotSameAs(traits)

        val traits = Traits().putAnonymousId("foo")
        context.setTraits(traits)
        assertThat(context.traits()).isEqualTo(traits).isNotSameAs(traits)
    }

    @Test
    fun campaign() {
        val campaign = AnalyticsContext.Campaign()

        campaign.putName("campaign-name")
        assertThat(campaign.name()).isEqualTo("campaign-name")

        campaign.putSource("campaign-source")
        assertThat(campaign.source()).isEqualTo("campaign-source")

        campaign.putMedium("campaign-medium")
        assertThat(campaign.medium()).isEqualTo("campaign-medium")

        campaign.putTerm("campaign-term")
        assertThat(campaign.term()).isEqualTo("campaign-term")
        assertThat(campaign.tern()).isEqualTo("campaign-term")

        campaign.putContent("campaign-content")
        assertThat(campaign.content()).isEqualTo("campaign-content")

        context.putCampaign(campaign)
        assertThat(context.campaign()).isEqualTo(campaign)
    }

    @Test
    fun device() {
        val device = AnalyticsContext.Device()

        device.putAdvertisingInfo("adId", true)
        assertThat(device).containsEntry("advertisingId", "adId")
        assertThat(device).containsEntry("adTrackingEnabled", true)
    }

    @Test
    fun location() {
        val location = AnalyticsContext.Location()

        location.putLatitude(37.7672319)
        assertThat(location.latitude()).isEqualTo(37.7672319)

        location.putLongitude(-122.404324)
        assertThat(location.longitude()).isEqualTo(-122.404324)

        location.putSpeed(88.0)
        assertThat(location.speed()).isEqualTo(88.0)

        location.putValue("city", "San Francisco")
        assertThat(location).containsEntry("city", "San Francisco")

        context.putLocation(location)
        assertThat(context.location()).isEqualTo(location)
    }

    @Test
    fun referrer() {
        val referrer = AnalyticsContext.Referrer()

        referrer.putId("referrer-id")
        assertThat(referrer.id()).isEqualTo("referrer-id")

        referrer.putLink("referrer-link")
        assertThat(referrer.link()).isEqualTo("referrer-link")

        referrer.putName("referrer-name")
        assertThat(referrer.name()).isEqualTo("referrer-name")

        referrer.putType("referrer-type")
        assertThat(referrer.type()).isEqualTo("referrer-type")

        referrer.putUrl("referrer-url")
        assertThat(referrer.url()).isEqualTo("referrer-url")

        context.putReferrer(referrer)
        assertThat(context).containsEntry("referrer", referrer)
    }

    @Test
    fun network() {
        val application = mock(Context::class.java)
        val manager = mock(ConnectivityManager::class.java)
        whenever(application.getSystemService(CONNECTIVITY_SERVICE)).thenReturn(manager)
        context.putNetwork(application)

        assertThat(context)
            .containsEntry(
                "network",
                ImmutableMap.Builder<Any, Any>()
                    .put("wifi", false)
                    .put("carrier", "unknown")
                    .put("bluetooth", false)
                    .put("cellular", false)
                    .build()
            )
    }

    @Test
    fun deviceIdFetchedIn2Seconds() {
        val sharedPreferences = RuntimeEnvironment.application
            .getSharedPreferences("analytics-test-qaz", Context.MODE_PRIVATE)
        context = AnalyticsContext.create(RuntimeEnvironment.application, traits, true)
        val latch = CountDownLatch(1)
        val task = spy(GetDeviceIdTask(context, sharedPreferences, latch))

        doAnswer {
            "randomUUID"
        }.`when`(task).deviceId

        task.execute()
        latch.await()

        assertThat(context.getValueMap("device"))
            .containsKey("id")
        assertThat(context.getValueMap("device"))
            .containsEntry("id", "randomUUID")
    }

    @Test
    fun randomUUIDGeneratedAsDeviceIdAfter2Seconds() {
        val sharedPreferences = RuntimeEnvironment.application
            .getSharedPreferences("analytics-test-qaz", Context.MODE_PRIVATE)
        context = AnalyticsContext.create(RuntimeEnvironment.application, traits, true)
        val latch = CountDownLatch(1)
        val task = spy(GetDeviceIdTask(context, sharedPreferences, latch))

        doAnswer {
            Thread.sleep(3000)
            "randomUUID"
        }.`when`(task).deviceId

        task.execute()
        latch.await()

        assertThat(context.getValueMap("device"))
            .containsKey("id")
        // a random uuid should be generated to override the default empty value
        assertThat(context.getValueMap("device"))
            .doesNotContainEntry("id", "")
    }
}
