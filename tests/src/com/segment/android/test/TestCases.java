package com.segment.android.test;


import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import com.segment.android.models.Alias;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Batch;
import com.segment.android.models.Context;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Providers;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;

@SuppressWarnings("serial")
public class TestCases {
	
	private static Random random = new Random();
	
	public static Calendar calendar () {
		Calendar calendar = new GregorianCalendar();
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		calendar.set(Calendar.MILLISECOND, (int) (System.currentTimeMillis() + random.nextInt(10000)));
		return calendar;
	}
	
	public static Identify identify () {
		return new Identify(
			UUID.randomUUID().toString(),
			"ilya@segment.io", 
			new Traits(
					"name","Achilles", 
					"email", "achilles@segment.io", 
					"subscriptionPlan", "Premium", 
					"friendCount", 29, 
					"company", new EasyJSONObject()
						.put("name", "Company, inc.")), 
			calendar(),
			new Context().setIp("192.168.1.1"));
	}
	
	public static Group group () {
		return new Group(
			UUID.randomUUID().toString(),
			"ilya@segment.io", 
			"segmentio_id",
			new Traits(
				"name","Segment.io", 
				"plan", "Premium"), 
			calendar(),
			new Context().setIp("192.168.1.1"));
	}
	
	public static Track track () {
		return new Track(
			UUID.randomUUID().toString(),
			"ilya@segment.io", "Played a Song on Android", 
			new Props(
				"name", "Achilles",
				"revenue", 39.95,
				"shippingMethod", "2-day"),
				calendar(), 
				new Context()
					.setIp("192.168.1.1")
					.setProviders(new Providers()
						.setDefault(true)
						.setEnabled("Mixpanel", false)
						.setEnabled("KISSMetrics", true)
						.setEnabled("Google Analytics", true)));
	}
	
	public static Screen screen () {
		return new Screen(
			UUID.randomUUID().toString(),
			"ilya@segment.io", 
			"Login Page", 
			new Props(
				"logged-in", true,
				"type", "teacher"),
				calendar(), 
				new Context()
					.setIp("192.168.1.1")
					.setProviders(new Providers()
						.setDefault(true)
						.setEnabled("Mixpanel", false)
						.setEnabled("KISSMetrics", true)
						.setEnabled("Google Analytics", true)));
	}
	
	public static Alias alias () {
		return new Alias("from", "to", null, null);
	}
	
	public static Batch batch (String writeKey) {
		Batch batch = new Batch(writeKey, new LinkedList<BasePayload>() {{
			this.add(identify());
			this.add(track());
			this.add(alias());
		}});
		
		// set a global context
		batch.setContext(new Context("userAgent", "something"));
		
		return batch;
	}
	
	public static BasePayload random() {
		
		switch(random.nextInt(5)) {
			case 0:
				return identify();
			case 1: 
				return track();
			case 2: 
				return group();
			case 3: 
				return screen();
			default:
				return alias();
		}
	}
	
}
