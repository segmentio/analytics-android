package com.segment.analytics;

import com.segment.analytics.core.tests.BuildConfig;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = NONE)
public class ProjectSettingsTest {

  @Test public void deserialization() throws IOException {
    Cartographer cartographer = Cartographer.INSTANCE;
    String projectSettingsJson = "{\n"
        + "  \"Amplitude\": {\n"
        + "    \"trackNamedPages\": true,\n"
        + "    \"trackCategorizedPages\": true,\n"
        + "    \"trackAllPages\": false,\n"
        + "    \"apiKey\": \"x\"\n"
        + "  },\n"
        + "  \"Flurry\": {\n"
        + "    \"apiKey\": \"x\",\n"
        + "    \"captureUncaughtExceptions\": false,\n"
        + "    \"useHttps\": true,\n"
        + "    \"sessionContinueSeconds\": 10\n"
        + "  },\n"
        + "  \"Mixpanel\": {\n"
        + "    \"people\": true,\n"
        + "    \"token\": \"x\",\n"
        + "    \"trackAllPages\": false,\n"
        + "    \"trackCategorizedPages\": true,\n"
        + "    \"trackNamedPages\": true,\n"
        + "    \"increments\": [\n"
        + "      \n"
        + "    ],\n"
        + "    \"legacySuperProperties\": false\n"
        + "  },\n"
        + "  \"Segment.io\": {\n"
        + "    \"apiKey\": \"x\"\n"
        + "  }\n"
        + "}";
    ProjectSettings projectSettings =
        ProjectSettings.create(cartographer.fromJson(projectSettingsJson));

    assertThat(projectSettings).hasSize(5).containsKey("timestamp").containsKey("Segment.io");

    try {
      projectSettings.put("foo", "bar");
      fail("projectSettings should be immutable");
    } catch (UnsupportedOperationException ignored) {
    }
  }
}
