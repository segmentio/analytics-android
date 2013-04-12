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

public class AnalyticsTest extends BaseTest {

	
	private static String userId;
	private static int insertAttempts = 0;
	private static int flushAttempts = 0;
	
	@Override
	protected void setUp() {
		super.setUp();
		
		userId = "android_user_" + (new Random()).nextInt(999999);
		
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
		
		Assert.assertEquals(expected, statistics.getIdentifies().getCount());
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
		
		AnalyticsStatistics statistics = Analytics.getStatistics();
		
		Assert.assertEquals(expected, statistics.getTracks().getCount());
		Assert.assertEquals(insertAttempts, statistics.getInsertAttempts().getCount());
		
		Analytics.flush(false);

		flushAttempts += 1;
		
		Assert.assertEquals(flushAttempts, statistics.getFlushAttempts().getCount());
		
		Assert.assertEquals(insertAttempts, statistics.getSuccessful().getCount());
	}
	
	@Test
	public void testAlias() {
		
	}
	
	
}
