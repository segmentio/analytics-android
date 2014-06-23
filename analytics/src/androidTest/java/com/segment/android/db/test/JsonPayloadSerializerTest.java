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
import junit.framework.Assert;
import org.junit.Test;

public class JsonPayloadSerializerTest extends AndroidTestCase {

  private static JsonPayloadSerializer serializer;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    serializer = new JsonPayloadSerializer();
  }

  @Test
  public void testIdentify() {
    Identify identify = TestCases.identify();
    String json = serializer.serialize(identify);
    BasePayload got = serializer.deserialize(json);
    Assert.assertEquals(identify, got);
  }

  @Test
  public void testGroup() {
    Group group = TestCases.group();
    String json = serializer.serialize(group);
    BasePayload got = serializer.deserialize(json);
    Assert.assertEquals(group, got);
  }

  @Test
  public void testTrack() {
    Track track = TestCases.track();
    String json = serializer.serialize(track);
    BasePayload got = serializer.deserialize(json);
    Assert.assertEquals(track, got);
  }

  @Test
  public void testScreen() {
    Screen screen = TestCases.screen();
    String json = serializer.serialize(screen);
    BasePayload got = serializer.deserialize(json);
    Assert.assertEquals(screen, got);
  }

  @Test
  public void testAlias() {
    Alias alias = TestCases.alias();
    String json = serializer.serialize(alias);
    BasePayload got = serializer.deserialize(json);
    Assert.assertEquals(alias, got);
  }
}
