package com.segment.analytics

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings.Secure
import com.segment.analytics.integrations.Logger
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GetAdvertisingIdTest {

  @Test
  @Throws(Exception::class)
  fun getAdvertisingId() {
    val latch = CountDownLatch(1)
    val traits: Traits = Traits.create()
    val context: AnalyticsContext =
        AnalyticsContext.create(RuntimeEnvironment.application, traits, true)
    val task =
        GetAdvertisingIdTask(context, latch, Logger.with(Analytics.LogLevel.VERBOSE))
    task.execute(RuntimeEnvironment.application)
    latch.await()
    Assertions.assertThat(context.device()).doesNotContainKey("advertisingId")
  }

  @Test
  @Throws(Exception::class)
  fun getAdvertisingIdAmazonFireOSLimitAdTracking1() {
    val context: Context = RuntimeEnvironment.application
    val contentResolver: ContentResolver = context.contentResolver
    Secure.putInt(contentResolver, "limit_ad_tracking", 1)
    val latch = CountDownLatch(1)

    val traits = Traits.create()
    val analyticsContext =
        AnalyticsContext.create(RuntimeEnvironment.application, traits, true)

    val task =
        GetAdvertisingIdTask(analyticsContext, latch, Logger.with(Analytics.LogLevel.VERBOSE))
    task.execute(context)
    latch.await()

    Assertions.assertThat(analyticsContext.device()).doesNotContainKey("advertisingId")
    Assertions.assertThat(analyticsContext.device()).containsEntry("adTrackingEnabled", false)
  }

  @Test
  @Throws(Exception::class)
  fun getAdvertisingIdAmazonFireOSLimitAdTracking0() {
    val context: Context = RuntimeEnvironment.application
    val contentResolver: ContentResolver = context.contentResolver
    Secure.putInt(contentResolver,"limit_ad_tracking", 0)
    Secure.putString(contentResolver, "advertising_id", "df07c7dc-cea7-4a89-b328-810ff5acb15d")

    val latch = CountDownLatch(1)

    val traits = Traits.create()
    val analyticsContext =
        AnalyticsContext.create(RuntimeEnvironment.application, traits, true)

    val task = GetAdvertisingIdTask(analyticsContext, latch, Logger.with(Analytics.LogLevel.VERBOSE))
    task.execute(context)
    latch.await()

    Assertions.assertThat(analyticsContext.device())
        .containsEntry("advertisingId", "df07c7dc-cea7-4a89-b328-810ff5acb15d")
    Assertions.assertThat(analyticsContext.device()).containsEntry("adTrackingEnabled", true)
  }
}