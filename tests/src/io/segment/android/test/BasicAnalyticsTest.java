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

	
	@Override
	protected void setUp() {
		super.setUp();
		
		Log.w("AnalyticsTest", "Analytics Test using userId: " + userId);
	}
	
	@Test
	public void testIdentify() {

		AnalyticsStatistics statistics = Analytics.getStatistics();
		
		int insertAttempts = statistics.getInsertAttempts().getCount();
		int identifyAttempts = statistics.getIdentifies().getCount();
		int flushAttempts =  statistics.getFlushAttempts().getCount();
		int successful = statistics.getSuccessful().getCount();

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
		
		Assert.assertEquals(identifyAttempts + 5, statistics.getIdentifies().getCount());
		Assert.assertEquals(insertAttempts + 5, statistics.getInsertAttempts().getCount());
		
		Analytics.flush(false);
		
		Assert.assertEquals(flushAttempts + 1, statistics.getFlushAttempts().getCount());
		
		Assert.assertEquals(successful + 5, statistics.getSuccessful().getCount());
	}
	
	@Test
	public void testTrack() {

		AnalyticsStatistics statistics = Analytics.getStatistics();
		
		int insertAttempts = statistics.getInsertAttempts().getCount();
		int trackAttempts = statistics.getTracks().getCount();
		int flushAttempts =  statistics.getFlushAttempts().getCount();
		int successful = statistics.getSuccessful().getCount();
		
		Analytics.track("Android: UserId Saved Action");
		
		Analytics.track("Android: UserId Not Saved Action");
		
		Analytics.track("Android: First Event Properties Event", new EventProperties(
			"Mickey Mouse", 4,
			"Donnie", "Darko"
		));
		
		Analytics.track("Android: With Calendar", new EventProperties(),  TestCases.calendar);
		
		Analytics.track("Android: With Context", new EventProperties(),  TestCases.calendar, new Context(
			"providers", new EasyJSONObject(
					"Mixpanel", true,
					"KISSMetrics", true
		)));
		
		Assert.assertEquals(trackAttempts + 5, statistics.getTracks().getCount());
		Assert.assertEquals(insertAttempts + 5, statistics.getInsertAttempts().getCount());
		
		Analytics.flush(false);
		
		Assert.assertEquals(flushAttempts + 1, statistics.getFlushAttempts().getCount());
		
		Assert.assertEquals(successful + 5, statistics.getSuccessful().getCount());
	}
	
	@Test
	public void testAlias() {

		AnalyticsStatistics statistics = Analytics.getStatistics();
		
		int insertAttempts = statistics.getInsertAttempts().getCount();
		int aliasAttempts = statistics.getAlias().getCount();
		int flushAttempts =  statistics.getFlushAttempts().getCount();
		int successful = statistics.getSuccessful().getCount();
		
		String from = Analytics.getSessionId();
		String to = "android_user_" + (new Random()).nextInt(999999);

		Analytics.setSessionId(from);
		
		Log.w("AnalyticsTest", "Aliasing : " + from + " => " + to);
		
		Analytics.track("Anonymous Event");
		
		Analytics.alias(from, to);
		
		Analytics.identify(to, new Traits(
				"Crazay", "Duh"
		));
		
		Analytics.track("Identified Event");
		
		Assert.assertEquals(aliasAttempts + 1, statistics.getAlias().getCount());
		Assert.assertEquals(insertAttempts + 4, statistics.getInsertAttempts().getCount());
		
		Analytics.flush(false);
		
		Assert.assertEquals(flushAttempts + 1, statistics.getFlushAttempts().getCount());
		
		Assert.assertEquals(successful + 4, statistics.getSuccessful().getCount());
	}
	
	
}
