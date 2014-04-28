package io.segment.android.request.test;

import io.segment.android.Logger;
import io.segment.android.models.BasePayload;
import io.segment.android.models.Batch;
import io.segment.android.models.Context;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Identify;
import io.segment.android.models.Traits;
import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.test.BaseTest;
import io.segment.android.test.Constants;
import io.segment.android.test.TestCases;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

// The big question: requester or requestor? : 
// http://english.stackexchange.com/questions/29254/whats-the-difference-between-requester-and-requestor
public class BasicRequesterTest extends BaseTest {

	private IRequester requester;
	
	@Override
	protected void setUp() {
		super.setUp();
		
		requester = new BasicRequester();
	}
	
	@Test
	public void testSimpleBatchRequest () {
		HttpResponse response = requester.send(TestCases.batch(Constants.WRITE_KEY));
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
	}
	
	@Test
	public void testUTF8Characters () {
		
		Identify identify = new Identify("2832-2-3", "some_user", new Traits(
				"carrier", "GR COSMOTE",
				"language", "Ελληνικά",
				"country", "Ελλάδα"), 
			Calendar.getInstance(), new Context());
		
		List<BasePayload> items = new LinkedList<BasePayload>();
		items.add(identify);
		
		
		Batch batch = new Batch(Constants.WRITE_KEY, items);
		
		HttpResponse response = requester.send(batch);
		
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
	}
	
	
	@Test
	public void testSimpleSettingsRequest() {
		
		EasyJSONObject settings = requester.fetchSettings();
		
		Assert.assertNotNull(settings);
		Assert.assertTrue(settings.length() > 0);
		
		String[] keys = {"Segment.io"};
		
		for (String key : keys) {
			if (settings.getObject(key) == null) Logger.e(key + " is not in settings!");
			Assert.assertNotNull(settings.getObject(key));
		}
	}
	
}
