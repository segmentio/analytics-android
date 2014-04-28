package io.segment.android.flush.test;

import io.segment.android.Analytics;
import io.segment.android.models.BasePayload;
import io.segment.android.models.Batch;
import io.segment.android.request.BasicRequester;
import io.segment.android.test.BaseTest;
import io.segment.android.test.TestCases;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.junit.Test;

import android.util.Log;

public class DuplicateTest extends BaseTest {

	public static final String TAG = "DuplicateTest";
	
	public class DuplicateCountingRequester extends BasicRequester {
		
		private final Set<String> requestIds = new HashSet<String>();
		private final AtomicInteger duplicates = new AtomicInteger();
		
		@Override
		public HttpResponse send(Batch batch) {
			for (BasePayload payload : batch.getBatch()) {
				if (requestIds.contains(payload.getRequestId())) {
					// we've already sent this action
					duplicates.addAndGet(1);
					Log.w(TAG, "Detected duplicate " + payload.getRequestId());
				} else {
					// we haven't seen this action, lets remember it
					requestIds.add(payload.getRequestId());
				}
			}
			return super.send(batch);
		}
		
		public int countDuplicates () {
			return duplicates.get();
		}
	}
	
	@Test
	public void testForDuplicates() throws InterruptedException {
		int inserts = 200;
		int flushesPerInsert = 2;
		
		// set a custom counting duplicate requester
		DuplicateCountingRequester requester = new DuplicateCountingRequester();
		Analytics.setRequester(requester);
		
		for (int i = 0; i < inserts; i += 1) {
			// enqueue a random action
			Analytics.enqueue(TestCases.random());
			Log.i(TAG, "Enqueued message " + (i+1) + " / " + inserts);
			
			// trigger a series of flushes for every insert
			for (int j = 0; j < flushesPerInsert; j += 1) {
				Analytics.flush(true);
			}
			
			// and wait for some stuff to happen
			Thread.sleep(30);
		}
		
		Log.i(TAG, "Final blocking flush ..");
		// now let's block and wait for everything to send
		Analytics.flush(false);
		Log.i(TAG, "Detected " + requester.countDuplicates() + " duplicates.");
		
		// check that we haven't seen any duplicates
		Assert.assertEquals(0,  requester.countDuplicates());
	}
}