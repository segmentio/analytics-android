package io.segment.android.request.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.test.BaseTest;
import io.segment.android.test.TestCases;

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
		HttpResponse response = requester.send(TestCases.batch);
		
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
	}
	
	@Test
	public void testSimpleSettingsRequest() {
		
		EasyJSONObject settings = requester.fetchSettings();
		
		Assert.assertNotNull(settings);
		Assert.assertTrue(settings.length() > 0);
		
		String[] keys = {
			"Customer.io", "HubSpot", "KISSmetrics", "Olark", "Keen IO", "Segment.io",
			"Klaviyo", "Salesforce", "Librato", "Intercom", "Mixpanel", "Woopra",
			"HelpScount", "Pardot", "Marketo", "Google Analytics", "Chartbeat", "Vero"
		};
		
		for (String key : keys) {
			Assert.assertNotNull(settings.getObject(key));
		}
	}
	
}
