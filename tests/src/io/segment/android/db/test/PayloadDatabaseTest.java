package io.segment.android.db.test;

import io.segment.android.db.PayloadDatabase;
import io.segment.android.models.BasePayload;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.test.BaseTest;
import io.segment.android.test.TestCases;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import android.util.Pair;

public class PayloadDatabaseTest extends BaseTest {

	private PayloadDatabase database;
	
	@Override
	protected void setUp() {
		super.setUp();
		
		database = PayloadDatabase.getInstance(context);
		
		// clean out database beforehand from unsuccessful tests
		database.removeEvents(0, 99999999);
	}
	
	@Test
	public void testSingle() {
		
		List<Pair<Long, BasePayload>> events = database.getEvents(50);
		Assert.assertEquals(0, events.size());
		
		boolean success = database.addPayload(TestCases.identify);
		Assert.assertTrue(success);
		
		events = database.getEvents(50);
		Assert.assertEquals(1, events.size());
		boolean equals = EasyJSONObject.equals(TestCases.identify, events.get(0).second);
		Assert.assertTrue(equals);
		
		// check that our row counter is correct
		Assert.assertEquals(1, database.getRowCount());
		
		// now let's remove that event
		long minId = events.get(0).first;
		long maxId = events.get(events.size() - 1).first;
		int removed = database.removeEvents(minId, maxId);
		Assert.assertEquals(1, removed);
		// now let's check that there's nothing in the database
		events = database.getEvents(50);
		Assert.assertEquals(0, events.size());

		// check that our row counter was decremented by the remove
		Assert.assertEquals(0, database.getRowCount());
	}

	@Test
	public void testPerformance() {
		
		int msPerInsert = 150;
		int added = 100;
		
		List<BasePayload> payloads = new LinkedList<BasePayload>();
		
		long start = System.currentTimeMillis();
		
		for (int i = 0; i < added; i += 1) {
			BasePayload payload = TestCases.random(); 
			payloads.add(payload);
			boolean success = database.addPayload(payload);
			Assert.assertTrue(success);
		}
		
		long duration = System.currentTimeMillis() - start;
				
		Assert.assertTrue(duration < msPerInsert * added);

		// check that our row counter is correct
		Assert.assertEquals(added, database.getRowCount());
		
		int left = added;
		int queryLimit = 50;
		
		while (left > 0) {
			// check that we can get those items 
			List<Pair<Long, BasePayload>> events = database.getEvents(queryLimit);
			
			// we expect the full query size
			int expected = queryLimit;
			// unless the query is larger than what we have left
			if (left - queryLimit < 0) expected = left;
			
			Assert.assertEquals(events.size(), expected);
			
			// now let's remove these events
			long minId = events.get(0).first;
			long maxId = events.get(events.size() - 1).first;
			int removed = database.removeEvents(minId, maxId);
			Assert.assertEquals(expected, removed);
			
			left -= queryLimit;
		}
		
		// now let's check that there's nothing in the database
		List<Pair<Long, BasePayload>> events = database.getEvents(queryLimit);
		Assert.assertEquals(0, events.size());
		
		// make sure the database row count is 0
		Assert.assertEquals(0, database.getRowCount());

	}
	
}
