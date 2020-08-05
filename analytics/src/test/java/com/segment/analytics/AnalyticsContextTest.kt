package com.segment.analytics

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import com.google.common.collect.ImmutableMap
import com.segment.analytics.Utils.createContext
import com.segment.analytics.core.BuildConfig
import org.assertj.core.api.Assertions
import org.assertj.core.data.MapEntry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
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
    Assertions.assertThat(context)
        .containsKeys("app", "device", "library", "locale", "network", "os", "screen", "timezone", "traits")
    Assertions.assertThat(context).containsEntry("userAgent", "undefined")

    Assertions.assertThat(context.getValueMap("app"))
        .containsEntry("name", "org.robolectric.default")
    Assertions.assertThat(context.getValueMap("app"))
        .containsEntry("version", "undefined")
    Assertions.assertThat(context.getValueMap("app"))
        .containsEntry("namespace", "org.robolectric.default")
    Assertions.assertThat(context.getValueMap("app"))
        .containsEntry("build", "0")

    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("id", "unknown")
    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("manufacturer", "unknown")
    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("model", "unknown")
    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("name", "unknown")
    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("type", "android")

    Assertions.assertThat(context.getValueMap("library"))
        .containsEntry("name", "analytics-android")
    Assertions.assertThat(context.getValueMap("library"))
        .containsEntry("version", BuildConfig.VERSION_NAME)

    //todo: mock network state?
    Assertions.assertThat(context.getValueMap("network")).isEmpty()

    Assertions.assertThat(context.getValueMap("os"))
        .containsEntry("name", "Android")
    Assertions.assertThat(context.getValueMap("os"))
        .containsEntry("version", "4.1.2_r1")

    Assertions.assertThat(context.getValueMap("screen"))
        .containsEntry("density", 1.5f)
    Assertions.assertThat(context.getValueMap("screen"))
        .containsEntry("width", 480)
    Assertions.assertThat(context.getValueMap("screen"))
        .containsEntry("height", 800)
  }

  @Test
  fun createWithoutDeviceIdCollection() {
    context = AnalyticsContext.create(RuntimeEnvironment.application, traits, false)

    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("id", traits.anonymousId())
    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("manufacturer", "unknown")
    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("model", "unknown")
    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("name", "unknown")
    Assertions.assertThat(context.getValueMap("device"))
        .containsEntry("type", "android")
  }

  @Test
  fun copyReturnsSameMappings() {
    val copy = context.unmodifiableCopy()
    Assertions.assertThat(copy).hasSameSizeAs(context).isNotSameAs(context).isEqualTo(context)
    for ((key, value) in context) {
      Assertions.assertThat(copy).contains(MapEntry.entry(key, value))
    }
  }

  @Test
  fun copyIsImmutable() {
    val copy = context.unmodifiableCopy()

    try {
      copy["foo"] = "bar"
      Assertions.fail("Inserting into copy should throw UnsupportedOperationException")
    } catch(expected: UnsupportedOperationException) {
    }
  }

  @Test
  fun traitsAreCopied() {
    Assertions.assertThat(context.traits()).isEqualTo(traits).isNotSameAs(traits)

    val traits = Traits().putAnonymousId("foo")
    context.setTraits(traits)
    Assertions.assertThat(context.traits()).isEqualTo(traits).isNotSameAs(traits)
  }

  @Test
  fun campaign() {
    val campaign = AnalyticsContext.Campaign()

    campaign.putName("campaign-name")
    Assertions.assertThat(campaign.name()).isEqualTo("campaign-name")

    campaign.putSource("campaign-source")
    Assertions.assertThat(campaign.source()).isEqualTo("campaign-source")

    campaign.putMedium("campaign-medium")
    Assertions.assertThat(campaign.medium()).isEqualTo("campaign-medium")

    campaign.putTerm("campaign-term")
    Assertions.assertThat(campaign.term()).isEqualTo("campaign-term")
    Assertions.assertThat(campaign.tern()).isEqualTo("campaign-term")

    campaign.putContent("campaign-content")
    Assertions.assertThat(campaign.content()).isEqualTo("campaign-content")

    context.putCampaign(campaign)
    Assertions.assertThat(context.campaign()).isEqualTo(campaign)
  }

  @Test
  fun device() {
    val device = AnalyticsContext.Device()

    device.putAdvertisingInfo("adId", true)
    Assertions.assertThat(device).containsEntry("advertisingId", "adId")
    Assertions.assertThat(device).containsEntry("adTrackingEnabled", true)
  }

  @Test
  fun location() {
    val location = AnalyticsContext.Location()

    location.putLatitude(37.7672319)
    Assertions.assertThat(location.latitude()).isEqualTo(37.7672319)

    location.putLongitude(-122.404324)
    Assertions.assertThat(location.longitude()).isEqualTo(-122.404324)

    location.putSpeed(88.0)
    Assertions.assertThat(location.speed()).isEqualTo(88.0)

    location.putValue("city", "San Francisco")
    Assertions.assertThat(location).containsEntry("city", "San Francisco")

    context.putLocation(location)
    Assertions.assertThat(context.location()).isEqualTo(location)
  }

  @Test
  fun referrer() {
    val referrer = AnalyticsContext.Referrer()

    referrer.putId("referrer-id")
    Assertions.assertThat(referrer.id()).isEqualTo("referrer-id")

    referrer.putLink("referrer-link")
    Assertions.assertThat(referrer.link()).isEqualTo("referrer-link")

    referrer.putName("referrer-name")
    Assertions.assertThat(referrer.name()).isEqualTo("referrer-name")

    referrer.putType("referrer-type")
    Assertions.assertThat(referrer.type()).isEqualTo("referrer-type")

    referrer.putUrl("referrer-url")
    Assertions.assertThat(referrer.url()).isEqualTo("referrer-url")

    context.putReferrer(referrer)
    Assertions.assertThat(context).containsEntry("referrer", referrer)
  }

  @Test
  fun network() {
    val application = Mockito.mock(Context::class.java)
    val manager = Mockito.mock(ConnectivityManager::class.java)
    Mockito.`when`(application.getSystemService(CONNECTIVITY_SERVICE)).thenReturn(manager)
    context.putNetwork(application)

    Assertions.assertThat(context)
        .containsEntry(
            "network",
            ImmutableMap.Builder<Any, Any>()
                .put("wifi", false)
                .put("carrier", "unknown")
                .put("bluetooth", false)
                .put("cellular", false)
                .build())
  }
}