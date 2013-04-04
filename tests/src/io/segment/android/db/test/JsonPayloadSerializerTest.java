package io.segment.android.db.test;

import io.segment.android.db.JsonPayloadSerializer;
import io.segment.android.models.Alias;
import io.segment.android.models.BasePayload;
import io.segment.android.models.Batch;
import io.segment.android.models.Context;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Identify;
import io.segment.android.models.Providers;
import io.segment.android.models.Track;
import io.segment.android.models.Traits;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;

import junit.framework.Assert;

import org.junit.Test;

import android.test.AndroidTestCase;

public class JsonPayloadSerializerTest extends AndroidTestCase {

	private static Calendar calendar;
	private static JsonPayloadSerializer serializer;

	private static Identify identify;
	private static Track track;
	private static Alias alias;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		serializer = new JsonPayloadSerializer();
		
		calendar = new GregorianCalendar();
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		calendar.set(Calendar.YEAR, 2013);
		calendar.set(Calendar.MONTH, 4);
		calendar.set(Calendar.DATE, 5);
		calendar.set(Calendar.HOUR, 14);
		calendar.set(Calendar.MINUTE, 20);
		calendar.set(Calendar.SECOND, 15);
		
		identify = new Identify("ilya@segment.io", 
				new Traits(
						"name","Achilles", 
						"email", "achilles@segment.io", 
						"subscriptionPlan", "Premium", 
						"friendCount", 29, 
						"company", new EasyJSONObject()
							.put("name", "Company, inc.")), 
				calendar,
				new Context().setIp("192.168.1.1"));
		
		track = new Track("ilya@segment.io", "Played a Song", 
				new EventProperties(
						"name", "Achilles",
						"revenue", 39.95,
						"shippingMethod", "2-day"),
						calendar, 
						new Context()
							.setIp("192.168.1.1")
							.setProviders(new Providers()
								.setDefault(true)
								.setEnabled("Mixpanel", false)
								.setEnabled("KISSMetrics", true)
								.setEnabled("Google Analytics", true)));
		
		alias = new Alias("from", "to", null, null);
	}

	@Test
	public void testIdentify() {

		String json = serializer.serialize(identify);

		BasePayload got = serializer.deseralize(json);
		
		Assert.assertEquals(identify, got);
	}

	@Test
	public void testTrack() {
		
		String json = serializer.serialize(track);
		
		BasePayload got = serializer.deseralize(json);
		
		Assert.assertEquals(track, got);
	}

	@Test
	public void testAlias() {

		String json = serializer.serialize(alias);

		BasePayload got = serializer.deseralize(json);
		
		Assert.assertEquals(alias, got);
	}


	@Test
	public void testBatch() {

		@SuppressWarnings("serial")
		Batch batch = new Batch("testsecret", new LinkedList<BasePayload>() {{
			this.add(identify);
			this.add(track);
			this.add(alias);
		}});
		
		// set a global context
		batch.setContext(new Context("userAgent", "something"));
		
		String got = batch.toString();

		String expected = "{\"context\":{\"library\":\"analytics-android\",\"userAgent\":\"something\"},\"secret\":\"testsecret\",\"batch\":[{\"action\":\"identify\",\"context\":{\"library\":\"analytics-android\",\"ip\":\"192.168.1.1\"},\"timestamp\":\"2013-05-06T09:20:15+00:00\",\"traits\":{\"friendCount\":29,\"subscriptionPlan\":\"Premium\",\"email\":\"achilles@segment.io\",\"company\":{\"name\":\"Company, inc.\"},\"name\":\"Achilles\"},\"userId\":\"ilya@segment.io\"},{\"action\":\"track\",\"context\":{\"providers\":{\"Google Analytics\":true,\"KISSMetrics\":true,\"Mixpanel\":false,\"all\":true},\"library\":\"analytics-android\",\"ip\":\"192.168.1.1\"},\"timestamp\":\"2013-05-06T09:20:15+00:00\",\"properties\":{\"shippingMethod\":\"2-day\",\"revenue\":39.95,\"name\":\"Achilles\"},\"event\":\"Played a Song\",\"userId\":\"ilya@segment.io\"},{\"to\":\"to\",\"action\":\"alias\",\"from\":\"from\"}]}";
		
		Assert.assertEquals(expected, got);
	}

	
}
