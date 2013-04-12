package io.segment.android.test;

import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.request.test.TestCases;

import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

public class BasicRequestorTest extends BaseTest {

	private static IRequester requester = new BasicRequester();
	
	@Test
	public void simpleBatchRequestTest () {
		HttpResponse response = requester.send(TestCases.batch);
		
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
	}
	
}
