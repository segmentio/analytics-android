package com.segment.analytics;

import com.segment.analytics.core.tests.BuildConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class OptionsTest {

  Options options;

  @Before public void setUp() {
    options = new Options();
  }

  @Test public void disallowsDisablingSegmentIntegration() throws Exception {
    try {
      options.setIntegration("Segment.io", Randoms.nextBoolean());
      fail("shouldn't be able to set option for Segment integration.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("Segment integration cannot be enabled or disabled.");
    }
  }
}
