package com.segment.analytics

import com.google.common.collect.ImmutableMap
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OptionsTest {

  private lateinit var options: Options

  @Before
  fun setUp() { options = Options() }

  @Throws(Exception::class)
  @Test
  fun disallowsDisablingSegmentIntegration() {
    try{
      options.setIntegration("Segment.io", true)
      Assertions.fail("shouldn't be able to set option for Segment integration.")
    } catch(expected: IllegalArgumentException){
      Assertions.assertThat(expected)
          .hasMessage("Segment integration cannot be enabled or disabled.")
    }
  }

  @Throws(Exception::class)
  @Test
  fun setIntegration() {
    options.setIntegration("Mixpanel", true)
    options.setIntegration("All", false)
    options.setIntegration(Analytics.BundledIntegration.BUGSNAG, false)
    options.setIntegrationOptions("Amplitude",
        ImmutableMap.Builder<String, Any>().put("email", "foo").build())

    options.setIntegrationOptions(Analytics.BundledIntegration.TAPSTREAM,
        ImmutableMap.Builder<String, Any>().put("appId", "bar").build())

    Assertions.assertThat(options.integrations()).isEqualTo(
        ImmutableMap.Builder<String, Any>()
            .put("Mixpanel", true)
            .put("All", false)
            .put("Bugsnag", false)
            .put("Amplitude",
                ImmutableMap.Builder<String, Any>().put("email", "foo").build())
            .put("Tapstream",
                ImmutableMap.Builder<String, Any>().put("appId", "bar").build())
            .build())
  }

  @Test
  fun setOptions() {
    options.putContext("foo", "bar")
    options.putContext("library",
        ImmutableMap.Builder<String, Any>().put("name", "analytics-test").build())

    Assertions.assertThat(options.context()).isEqualTo(
        ImmutableMap.Builder<String, Any>()
            .put("foo", "bar")
            .put("library",
                ImmutableMap.Builder<String, Any>()
                    .put("name", "analytics-test").build())
            .build())
  }
}