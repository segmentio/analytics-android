package io.segment.android.test;

import io.segment.android.Analytics;
import io.segment.android.models.Context;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Traits;
import io.segment.android.stats.AnalyticsStatistics;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import android.util.Log;

public class BasicAnalyticsTest extends BaseTest {

	
	private static String userId = "android_user_" + (new Random()).nextInt(999999);

	private static int identifyAttempts = 0;
	private static int trackAttempts = 0;
	private static int aliasAttempts = 0;
	private static int insertAttempts = 0;
	private static int flushAttempts = 0;
	
	@Override
	protected void setUp() {
		super.setUp();
		
		Log.w("AnalyticsTest", "Analytics Test using userId: " + userId);
	}
	
	@Test
	public void testIdentify() {
		
		int expected = 5;
		
		Traits traits = new Traits(
			"username", userId,
			"baller", true
		);
		
		Analytics.identify(userId, traits);
		
		Analytics.identify(userId);
		
		Analytics.identify(new Traits(
			"username", userId,
			"baller", true,
			"just_user_id", true
		));
		
		Analytics.identify(traits, TestCases.calendar);
		
		Analytics.identify(traits, TestCases.calendar, new Context(
			"providers", new EasyJSONObject(
					"Mixpanel", true,
					"KISSMetrics", true
		)));
		
		AnalyticsStatistics statistics = Analytics.getStatistics();
		
		insertAttempts += expected;
		identifyAttempts += expected;
		
		Assert.assertEquals(identifyAttempts, statistics.getIdentifies().getCount());
		Assert.assertEquals(insertAttempts, statistics.getInsertAttempts().getCount());
		
		Analytics.flush(false);
		
		flushAttempts += 1;
		
		Assert.assertEquals(flushAttempts, statistics.getFlushAttempts().getCount());
		
		Assert.assertEquals(insertAttempts, statistics.getSuccessful().getCount());
	}
	
	@Test
	public void testTrack() {

		int expected = 5;
		
		Analytics.track("Android: UserId Saved Action");
		
		Analytics.track(userId, "Android: UserId Not Saved Action");
		
		Analytics.track("Android: First Event Properties Event", new EventProperties(
			"Mickey Mouse", 4,
			"Donnie", "Darko"
		));
		
		Analytics.track("Android: With Calendar", new EventProperties(),  TestCases.calendar);
		
		Analytics.track(userId, "Android: With Context", new EventProperties(),  TestCases.calendar, new Context(
			"providers", new EasyJSONObject(
					"Mixpanel", true,
					"KISSMetrics", true
		)));
		
		insertAttempts += expected;
		trackAttempts += expected;
		
		AnalyticsStatistics statistics = Analytics.getStatistics();
		
		Assert.assertEquals(trackAttempts, statistics.getTracks().getCount());
		Assert.assertEquals(insertAttempts, statistics.getInsertAttempts().getCount());
		
		Analytics.flush(false);

		flushAttempts += 1;
		
		Assert.assertEquals(flushAttempts, statistics.getFlushAttempts().getCount());
		
		Assert.assertEquals(insertAttempts, statistics.getSuccessful().getCount());
	}
	
	@Test
	public void testAlias() {
		
		String from = Analytics.getSessionId();
		String to = "android_user_" + (new Random()).nextInt(999999);

		Log.w("AnalyticsTest", "Aliasing : " + from + " => " + to);
		
		int expected = 4;
		
		Analytics.track(from, "Anonymous Event");
		
		Analytics.alias(from, to);
		
		Analytics.identify(to, new Traits(
				"Crazay", "Duh"
		));
		
		Analytics.track(to, "Identified Event");

		insertAttempts += expected;
		trackAttempts += 2;
		aliasAttempts += 1;
		identifyAttempts += 1;
		
		AnalyticsStatistics statistics = Analytics.getStatistics();
		
		Assert.assertEquals(aliasAttempts, statistics.getAlias().getCount());
		Assert.assertEquals(insertAttempts, statistics.getInsertAttempts().getCount());
		
		Analytics.flush(false);

		flushAttempts += 1;
		
		Assert.assertEquals(flushAttempts, statistics.getFlushAttempts().getCount());
		
		Assert.assertEquals(insertAttempts, statistics.getSuccessful().getCount());
	}
	
	
}
