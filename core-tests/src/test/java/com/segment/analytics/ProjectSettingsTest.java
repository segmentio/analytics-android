package com.segment.analytics;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class ProjectSettingsTest {

  @Test public void deserialization() throws IOException {
    String projectSettingsJson = "{\n"
        + "  \"Amplitude\": {\n"
        + "    \"trackNamedPages\": true,\n"
        + "    \"trackCategorizedPages\": true,\n"
        + "    \"trackAllPages\": false,\n"
        + "    \"apiKey\": \"ad3c426eb736d7442a65da8174bc1b1b\"\n"
        + "  },\n"
        + "  \"Flurry\": {\n"
        + "    \"apiKey\": \"8DY3D6S7CCWH54RBJ9ZM\",\n"
        + "    \"captureUncaughtExceptions\": false,\n"
        + "    \"useHttps\": true,\n"
        + "    \"sessionContinueSeconds\": 10\n"
        + "  },\n"
        + "  \"Mixpanel\": {\n"
        + "    \"people\": true,\n"
        + "    \"token\": \"f7afe0cb436685f61a2b203254779e02\",\n"
        + "    \"trackAllPages\": false,\n"
        + "    \"trackCategorizedPages\": true,\n"
        + "    \"trackNamedPages\": true,\n"
        + "    \"increments\": [\n"
        + "      \n"
        + "    ],\n"
        + "    \"legacySuperProperties\": false\n"
        + "  },\n"
        + "  \"Segment\": {\n"
        + "    \"apiKey\": \"l8v1ga655b\"\n"
        + "  }\n"
        + "}";
    ProjectSettings projectSettings =
        ProjectSettings.create(projectSettingsJson, System.currentTimeMillis());

    assertThat(projectSettings).hasSize(5);

    try {
      projectSettings.put("foo", "bar");
      fail("projectSettings should be immutable");
    } catch (UnsupportedOperationException ignored) {

    }
  }
}
