package io.segment.android.db.test;

import io.segment.android.db.JsonPayloadSerializer;
import io.segment.android.models.Alias;
import io.segment.android.models.BasePayload;
import io.segment.android.models.Group;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.test.TestCases;
import junit.framework.Assert;

import org.junit.Test;

import android.test.AndroidTestCase;

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
		BasePayload got = serializer.deseralize(json);
		Assert.assertEquals(identify, got);
	}

	@Test
	public void testGroup() {
		Group group = TestCases.group();
		String json = serializer.serialize(group);
		BasePayload got = serializer.deseralize(json);
		Assert.assertEquals(group, got);
	}
	
	@Test
	public void testTrack() {
		Track track = TestCases.track();
		String json = serializer.serialize(track);
		BasePayload got = serializer.deseralize(json);
		Assert.assertEquals(track, got);
	}
	
	@Test
	public void testScreen() {
		Screen screen = TestCases.screen();
		String json = serializer.serialize(screen);
		BasePayload got = serializer.deseralize(json);
		Assert.assertEquals(screen, got);
	}

	@Test
	public void testAlias() {
		Alias alias = TestCases.alias();
		String json = serializer.serialize(alias);
		BasePayload got = serializer.deseralize(json);
		Assert.assertEquals(alias, got);
	}
	
}
