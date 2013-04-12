package io.segment.android.info;

import junit.framework.Assert;

import org.junit.Test;

import android.test.AndroidTestCase;

public class SessionIdTest extends AndroidTestCase {

	private Info<String> info = new SessionId();
	
	@Test
	public void testGet() {
		String sessionId = info.get(this.getContext());
		Assert.assertNotNull(sessionId);
	} 
	
	
}
