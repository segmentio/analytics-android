package io.segment.android.db.test;

import io.segment.android.db.JsonPayloadSerializer;
import io.segment.android.models.BasePayload;
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

		String json = serializer.serialize(TestCases.identify);

		BasePayload got = serializer.deseralize(json);
		
		Assert.assertEquals(TestCases.identify, got);
	}

	@Test
	public void testTrack() {
		
		String json = serializer.serialize(TestCases.track);
		
		BasePayload got = serializer.deseralize(json);
		
		Assert.assertEquals(TestCases.track, got);
	}

	@Test
	public void testAlias() {

		String json = serializer.serialize(TestCases.alias);

		BasePayload got = serializer.deseralize(json);
		
		Assert.assertEquals(TestCases.alias, got);
	}


	@Test
	public void testBatch() {

		String got = TestCases.batch.toString();

		String expected = "{\"context\":{\"library\":\"analytics-android\",\"userAgent\":\"something\"},\"secret\":\"testsecret\",\"batch\":[{\"action\":\"identify\",\"context\":{\"library\":\"analytics-android\",\"ip\":\"192.168.1.1\"},\"timestamp\":\"2013-05-05T21:20:15+00:00\",\"traits\":{\"friendCount\":29,\"subscriptionPlan\":\"Premium\",\"email\":\"achilles@segment.io\",\"company\":{\"name\":\"Company, inc.\"},\"name\":\"Achilles\"},\"userId\":\"ilya@segment.io\"},{\"action\":\"track\",\"context\":{\"providers\":{\"Google Analytics\":true,\"KISSMetrics\":true,\"Mixpanel\":false,\"all\":true},\"library\":\"analytics-android\",\"ip\":\"192.168.1.1\"},\"timestamp\":\"2013-05-05T21:20:15+00:00\",\"properties\":{\"shippingMethod\":\"2-day\",\"revenue\":39.95,\"name\":\"Achilles\"},\"event\":\"Played a Song on Android\",\"userId\":\"ilya@segment.io\"},{\"to\":\"to\",\"action\":\"alias\",\"from\":\"from\"}]}";
		
		Assert.assertEquals(expected, got);
	}

	
}
