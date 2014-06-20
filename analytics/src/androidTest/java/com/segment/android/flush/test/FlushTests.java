package com.segment.android.flush.test;


import org.junit.Assert;
import org.junit.Test;

import com.segment.android.Analytics;
import com.segment.android.stats.AnalyticsStatistics;
import com.segment.android.test.BaseTest;
import com.segment.android.test.TestCases;

public class FlushTests extends BaseTest {

	@Test
	public void testEmptyFlush() {
		
		AnalyticsStatistics stats = Analytics.getStatistics();
		
		int flushAttempts = stats.getFlushAttempts().getCount();
		int requests = stats.getRequestTime().getCount();
		
		Analytics.flush(false);
		Analytics.flush(false);
		Analytics.flush(false);
		
		flushAttempts += 3;
		
		Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());
		Assert.assertEquals(0, requests);
	}
	
	@Test
	public void testFlushAtTrigger() { 

		int flushAt = Analytics.getOptions().getFlushAt();
		
		AnalyticsStatistics stats = Analytics.getStatistics();
		
		int flushAttempts = stats.getFlushAttempts().getCount();
		
		for (int i = 0; i < flushAt; i += 1) {
			Analytics.enqueue(TestCases.random());
		}
		
		// we expect that the flushing happened here
		flushAttempts += 1;
		
		// we want to wait until the flush actually happens
		Analytics.flush(false);
		flushAttempts += 1;
		
		Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());
	}
	

	@Test
	public void testFlushAfterTrigger() { 

		int flushAfter = Analytics.getOptions().getFlushAfter();
		
		AnalyticsStatistics stats = Analytics.getStatistics();
		
		int flushAttempts = stats.getFlushAttempts().getCount();
		int requests = stats.getRequestTime().getCount();
		
		Analytics.enqueue(TestCases.random());

		try {
			Thread.sleep(flushAfter + 250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// the flush after timer should have triggered a flush here
		flushAttempts += 1;
		requests += 1;
		
		// we want to wait until the flush actually happens
		Analytics.flush(false);
		flushAttempts += 1;

		Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());
		
		Assert.assertEquals(requests, stats.getRequestTime().getCount());
	}
	
}
