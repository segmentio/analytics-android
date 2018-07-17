package com.segment.analytics.integrations;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

public class ScreenPayloadTest {

  private ScreenPayload.Builder builder;

  @Before
  public void setUp() {
    builder = new ScreenPayload.Builder().userId("userId");
  }

  @Test
  public void name() {
    ScreenPayload payload = builder.name("name").build();
    assertThat(payload.name()).isEqualTo("name");
    assertThat(payload).containsEntry(ScreenPayload.NAME_KEY, "name");
  }

  @Test
  public void category() {
    ScreenPayload payload = builder.category("category").build();
    assertThat(payload.category()).isEqualTo("category");
    assertThat(payload).containsEntry(ScreenPayload.CATEGORY_KEY, "category");
  }

  @Test
  public void requiresNameOrCategory() {
    try {
      //noinspection CheckResult
      builder.build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("either name or category is required");
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
    ScreenPayload payload = builder.name("name").properties(ImmutableMap.of("foo", "bar")).build();
    assertThat(payload.properties()).isEqualTo(ImmutableMap.of("foo", "bar"));
    assertThat(payload).containsEntry(ScreenPayload.PROPERTIES_KEY, ImmutableMap.of("foo", "bar"));
  }

  @Test
  public void eventWithNoCategory() {
    ScreenPayload payload = builder.name("name").build();
    assertThat(payload.event()).isEqualTo("name");
  }

  @Test
  public void eventWithNoName() {
    ScreenPayload payload = builder.category("category").build();
    assertThat(payload.event()).isEqualTo("category");
  }
}
