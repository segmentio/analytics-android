package io.segment.android.request.test;

import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.test.BaseTest;
import io.segment.android.test.TestCases;

import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

// The big question requester or requestor? : 
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
	
}
