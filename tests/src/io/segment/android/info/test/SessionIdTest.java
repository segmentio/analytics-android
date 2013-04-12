package io.segment.android.info.test;

import io.segment.android.info.Info;
import io.segment.android.info.SessionId;
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
