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
