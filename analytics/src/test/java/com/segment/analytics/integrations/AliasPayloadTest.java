package com.segment.analytics.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class AliasPayloadTest {

  private AliasPayload.Builder builder;

  @Before
  public void setUp() {
    builder = new AliasPayload.Builder().previousId("previousId").userId("userId");
  }

  @Test
  public void previousId() {
    AliasPayload payload = builder.previousId("previous_id").build();
    assertThat(payload.previousId()).isEqualTo("previous_id");
    assertThat(payload).containsEntry(AliasPayload.PREVIOUS_ID_KEY, "previous_id");
  }

  @Test
  public void invalidPreviousIdThrows() {
    try {
      //noinspection CheckResult,ConstantConditions
      builder.previousId(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("previousId cannot be null or empty");
    }

    try {
      //noinspection CheckResult
      builder.previousId("");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("previousId cannot be null or empty");
    }
  }
}
