package com.segment.analytics;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import com.google.common.collect.ImmutableMap;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.TrackPayload;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class IntegrationOperationTest {

  @Mock Integration<Void> integration;

  @Before
  public void setUp() {
    initMocks(this);
  }

  private void track(TrackPayload payload, String name, Map<String, Object> settings) {
    IntegrationOperation.track(payload).run(name, integration, new ProjectSettings(settings));
  }

  @Test
  public void trackNoOptions() {
    TrackPayload payload = new TrackPayload.Builder().event("event").userId("userId").build();
    track(payload, "Mixpanel", Collections.<String, Object>emptyMap());
    verify(integration).track(payload);
  }

  @Test
  public void trackDisabledInOptions() {
    TrackPayload payload =
        new TrackPayload.Builder()
            .event("event")
            .userId("userId")
            .integrations(Collections.singletonMap("Mixpanel", false))
            .build();
    track(payload, "Mixpanel", Collections.<String, Object>emptyMap());
    verify(integration, never()).track(payload);
  }

  @Test
  public void trackAllDisabledInOptions() {
    TrackPayload payload =
        new TrackPayload.Builder()
            .event("event")
            .userId("userId")
            .integrations(Collections.singletonMap("All", false))
            .build();
    track(payload, "Mixpanel", Collections.<String, Object>emptyMap());
    verify(integration, never()).track(payload);
  }

  @Test
  public void trackAllDisabledInOptionsButIntegrationEnabled() {
    TrackPayload payload =
        new TrackPayload.Builder()
            .event("event")
            .userId("userId")
            .integrations(ImmutableMap.of("All", false, "Mixpanel", true))
            .build();
    track(payload, "Mixpanel", Collections.<String, Object>emptyMap());
    verify(integration).track(payload);
  }

  @Test
  public void trackAllDisabledInOptionsButIntegrationEnabledWithOptions() {
    TrackPayload payload =
        new TrackPayload.Builder()
            .event("event")
            .userId("userId")
            .integrations(ImmutableMap.of("All", false, "Mixpanel", Collections.emptyMap()))
            .build();
    track(payload, "Mixpanel", Collections.<String, Object>emptyMap());
    verify(integration).track(payload);
  }

  @Test
  public void trackPlanForEvent() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"Completed Order\": {}\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration).track(payload);
  }

  @Test
  public void trackWithOptionsAndWithoutEventPlan() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder()
            .event("Install Attributed")
            .userId("userId")
            .integrations(ImmutableMap.of("Mixpanel", false))
            .build();
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"Completed Order\": {}\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration, never()).track(payload);
  }

  @Test
  public void trackPlanForEventWithOptions() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder()
            .event("Install Attributed")
            .userId("userId")
            .integrations(Collections.singletonMap("All", false))
            .build();
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"Completed Order\": {}\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration, never()).track(payload);
  }

  @Test
  public void trackPlanDisabledEvent() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Amplitude",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"Install Attributed\": {\n"
                + "        \"enabled\": false\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration, never()).track(payload);
  }

  @Test
  public void trackPlanDisabledEventSendsToSegment() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Segment.io",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"Install Attributed\": {\n"
                + "        \"enabled\": false\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration).track(payload);
  }

  @Test
  public void trackPlanDisabledIntegration() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Amplitude",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"Install Attributed\": {\n"
                + "        \"integrations\": {\n"
                + "          \"Amplitude\": false\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration, never()).track(payload);
  }

  @Test
  public void trackPlanEnabledIntegration() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"Install Attributed\": {\n"
                + "        \"integrations\": {\n"
                + "          \"Mixpanel\": true\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration).track(payload);
  }

  @Test
  public void ignoresSegment() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Segment.io",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"Install Attributed\": {\n"
                + "        \"integrations\": {\n"
                + "          \"Segment.io\": false\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration).track(payload);
  }

  @Test
  public void defaultNewEventsEnabled() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Segment.io",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"__default\": {\n"
                + "        \"enabled\": true\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration).track(payload);
  }

  @Test
  public void defaultNewEventsDisabled() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"__default\": {\n"
                + "        \"enabled\": false\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration, never()).track(payload);
  }

  @Test
  public void defaultNewEventsDisabledSendToSegment() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Segment.io",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"__default\": {\n"
                + "        \"enabled\": false\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration).track(payload);
  }

  @Test
  public void eventPlanOverridesSchemaDefault() throws IOException {
    TrackPayload payload =
        new TrackPayload.Builder().event("Install Attributed").userId("userId").build();
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            "{\n"
                + "  \"plan\": {\n"
                + "    \"track\": {\n"
                + "      \"__default\": {\n"
                + "        \"enabled\": true\n"
                + "      },\n"
                + "      \"Install Attributed\": {\n"
                + "        \"enabled\": false\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"));
    verify(integration, never()).track(payload);
  }
}
