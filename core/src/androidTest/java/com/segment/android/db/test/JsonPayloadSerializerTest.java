/*
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

package com.segment.android.db.test;

import android.test.AndroidTestCase;
import com.segment.android.db.JsonPayloadSerializer;
import com.segment.android.models.Alias;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.test.TestCases;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class JsonPayloadSerializerTest extends AndroidTestCase {

  @Test
  public void testIdentify() {
    Identify identify = TestCases.identify();
    assertThat(serializeThenDeserialize(identify)).isEqualTo(identify);
  }

  @Test
  public void testGroup() {
    Group group = TestCases.group();
    assertThat(serializeThenDeserialize(group)).isEqualTo(group);
  }

  @Test
  public void testTrack() {
    Track track = TestCases.track();
    assertThat(serializeThenDeserialize(track)).isEqualTo(track);
  }

  @Test
  public void testScreen() {
    Screen screen = TestCases.screen();
    assertThat(serializeThenDeserialize(screen)).isEqualTo(screen);
  }

  @Test
  public void testAlias() {
    Alias alias = TestCases.alias();
    assertThat(serializeThenDeserialize(alias)).isEqualTo(alias);
  }

  BasePayload serializeThenDeserialize(BasePayload payload) {
    JsonPayloadSerializer serializer = new JsonPayloadSerializer();
    String json = serializer.serialize(payload);
    return serializer.deserialize(json);
  }
}
