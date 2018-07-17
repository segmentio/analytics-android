package com.segment.analytics.integrations;

import static com.segment.analytics.integrations.TrackPayload.EVENT_KEY;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

public class TrackPayloadTest {

  private TrackPayload.Builder builder;

  @Before
  public void setUp() {
    builder = new TrackPayload.Builder().userId("userId");
  }

  @Test
  public void invalidEventThrows() {
    try {
      //noinspection CheckResult,ConstantConditions
      new TrackPayload.Builder().event(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("event cannot be null or empty");
    }

    try {
      //noinspection CheckResult,ConstantConditions
      new TrackPayload.Builder().event("");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("event cannot be null or empty");
    }
  }

  @Test
  public void event() {
    TrackPayload payload = builder.event("event").build();
    assertThat(payload.event()).isEqualTo("event");
    assertThat(payload).containsEntry(EVENT_KEY, "event");
  }

  @Test
  public void eventIsRequired() {
    try {
      //noinspection CheckResult,ConstantConditions
      builder.build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("event cannot be null or empty");
    }
  }

  @Test
  public void invalidPropertiesThrows() {
    try {
      //noinspection CheckResult,ConstantConditions
      builder.properties(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("properties == null");
    }
  }

  @Test
  public void properties() {
    TrackPayload payload = builder.event("event").properties(ImmutableMap.of("foo", "bar")).build();
    assertThat(payload.properties()).isEqualTo(ImmutableMap.of("foo", "bar"));
    assertThat(payload).containsEntry(TrackPayload.PROPERTIES_KEY, ImmutableMap.of("foo", "bar"));
  }
}
