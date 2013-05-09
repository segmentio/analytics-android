package io.segment.android.test;

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
import java.util.Random;
import java.util.TimeZone;

@SuppressWarnings("serial")
public class TestCases {

	public static Calendar calendar;
	
	public static Identify identify;
	public static Track track;
	public static Alias alias;
	
	public static Batch batch;
	
	static {
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
		
		track = new Track("ilya@segment.io", "Played a Song on Android", 
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
		
		batch = new Batch("testsecret", new LinkedList<BasePayload>() {{
			this.add(identify);
			this.add(track);
			this.add(alias);
		}});
		
		// set a global context
		batch.setContext(new Context("userAgent", "something"));
	}
	
	private static Random random = new Random();
	
	public static BasePayload random() {
		
		switch(random.nextInt(3)) {
			case 0:
				return TestCases.identify;
			case 1: 
				return TestCases.track;
			default:
				return TestCases.alias;
		}
	}
	
}
