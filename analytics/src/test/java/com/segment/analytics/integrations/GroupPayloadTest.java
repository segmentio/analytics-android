/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
