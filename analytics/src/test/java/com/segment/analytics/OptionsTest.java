package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class OptionsTest {

  Options options;

  @Before
  public void setUp() {
    options = new Options();
  }

  @Test
  public void disallowsDisablingSegmentIntegration() throws Exception {
    try {
      options.setIntegration("Segment.io", true);
      fail("shouldn't be able to set option for Segment integration.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("Segment integration cannot be enabled or disabled.");
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void setIntegration() throws Exception {
    options.setIntegration("Mixpanel", true);
    options.setIntegration("All", false);
    options.setIntegration(Analytics.BundledIntegration.BUGSNAG, false);
    options.setIntegrationOptions(
        "Amplitude", new ImmutableMap.Builder<String, Object>().put("email", "foo").build());
    options.setIntegrationOptions(
        Analytics.BundledIntegration.TAPSTREAM,
        new ImmutableMap.Builder<String, Object>().put("appId", "bar").build());

    assertThat(options.integrations()) //
        .isEqualTo(
            new ImmutableMap.Builder<String, Object>() //
                .put("Mixpanel", true)
                .put("All", false)
                .put("Bugsnag", false)
                .put(
                    "Amplitude",
                    new ImmutableMap.Builder<String, Object>().put("email", "foo").build())
                .put(
                    "Tapstream",
                    new ImmutableMap.Builder<String, Object>().put("appId", "bar").build())
                .build());
  }
}
