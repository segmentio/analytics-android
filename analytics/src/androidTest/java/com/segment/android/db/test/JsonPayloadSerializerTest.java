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
