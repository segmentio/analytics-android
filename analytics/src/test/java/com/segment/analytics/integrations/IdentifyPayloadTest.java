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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

public class IdentifyPayloadTest {

  @Test
  public void invalidTraitsThrows() {
    try {
      //noinspection CheckResult,ConstantConditions
      new IdentifyPayload.Builder().traits(null);
      Assert.fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("traits == null");
    }
  }

  @Test
  public void traits() {
    IdentifyPayload payload =
        new IdentifyPayload.Builder()
            .userId("user_id")
            .traits(ImmutableMap.of("foo", "bar"))
            .build();
    assertThat(payload.traits()).isEqualTo(ImmutableMap.of("foo", "bar"));
    assertThat(payload).containsEntry(IdentifyPayload.TRAITS_KEY, ImmutableMap.of("foo", "bar"));
  }

  @Test
  public void requiresUserIdOrTraits() {
    try {
      //noinspection CheckResult
      new IdentifyPayload.Builder().anonymousId("anonymous_id").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("either userId or traits are required");
    }
  }
}
