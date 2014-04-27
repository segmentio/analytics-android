package io.segment.android.flush.test;

import io.segment.android.Analytics;
import io.segment.android.stats.AnalyticsStatistics;
import io.segment.android.test.BaseTest;
import io.segment.android.test.TestCases;

import org.junit.Assert;
import org.junit.Test;

public class FlushTests extends BaseTest {

	@Test
	public void testEmptyFlush() {
		
		AnalyticsStatistics stats = Analytics.getStatistics();
		
		int flushAttempts = stats.getFlushAttempts().getCount();
		int flushes = stats.getFlushTime().getCount();
		
		Analytics.flush(false);
		Analytics.flush(false);
		Analytics.flush(false);
		
		flushAttempts += 3;
		
		Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());
		Assert.assertEquals(0, flushes);
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
		int flushes = stats.getFlushTime().getCount();
		
		Analytics.enqueue(TestCases.random());

		try {
			Thread.sleep(flushAfter + 250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// the flush after timer should have triggered a flush here
		flushAttempts += 1;
		flushes += 1;
		
		// we want to wait until the flush actually happens
		Analytics.flush(false);
		flushAttempts += 1;

		Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());
		
		Assert.assertEquals(flushes, stats.getFlushTime().getCount());
	}
	
	@Test
	public void testTriggerTimerInteraction() throws InterruptedException {
	    AnalyticsStatistics stats = Analytics.getStatistics();
	    
	    int flushAfter = Analytics.getOptions().getFlushAfter();
	    int flushAt = Analytics.getOptions().getFlushAt();
	    int flushAttempts = stats.getFlushAttempts().getCount();
	    int flushes = stats.getFlushTime().getCount();
	    
	    for (int i = 0; i < flushAt + 5; i++) {
	        Analytics.enqueue(TestCases.random());
	    }
	    
	    // the flush after the trigger should have triggered a flush here
	    flushAttempts += 1;
	    flushes += 1;
	    try {
	        Thread.sleep(flushAfter + 250);
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    }
	    
	    // the flush after timer should have triggered a flush here
	    flushAttempts += 1;
	    flushes += 1;
	    Assert.assertEquals(flushes, stats.getFlushTime().getCount());

	    Assert.assertEquals(flushAttempts, stats.getFlushAttempts().getCount());
	}
}
