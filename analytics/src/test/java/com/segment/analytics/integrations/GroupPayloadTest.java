package com.segment.analytics.integrations;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GroupPayloadTest {

  private GroupPayload.Builder builder;

  @Before
  public void setUp() {
    builder = new GroupPayload.Builder().userId("userId");
  }

  @Test
  public void invalidGroupIdThrows() {
    try {
      //noinspection CheckResult,ConstantConditions
      new GroupPayload.Builder().groupId(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("groupId cannot be null or empty");
    }

    try {
      //noinspection CheckResult,ConstantConditions
      new GroupPayload.Builder().groupId("");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("groupId cannot be null or empty");
    }
  }

  @Test
  public void groupId() {
    GroupPayload payload = builder.groupId("group_id").build();
    assertThat(payload.groupId()).isEqualTo("group_id");
    assertThat(payload).containsEntry(GroupPayload.GROUP_ID_KEY, "group_id");
  }

  @Test
  public void groupIdIsRequired() {
    try {
      //noinspection CheckResult,ConstantConditions
      new GroupPayload.Builder().userId("user_id").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("groupId cannot be null or empty");
    }
  }

  @Test
  public void invalidTraitsThrows() {
    try {
      //noinspection CheckResult,ConstantConditions
      builder.traits(null);
      Assert.fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("traits == null");
    }
  }

  @Test
  public void traits() {
    GroupPayload payload =
        builder.groupId("group_id").traits(ImmutableMap.of("foo", "bar")).build();
    assertThat(payload.traits()).isEqualTo(ImmutableMap.of("foo", "bar"));
    assertThat(payload).containsEntry(GroupPayload.TRAITS_KEY, ImmutableMap.of("foo", "bar"));
  }
}
