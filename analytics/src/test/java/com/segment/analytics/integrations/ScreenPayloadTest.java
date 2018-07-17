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
