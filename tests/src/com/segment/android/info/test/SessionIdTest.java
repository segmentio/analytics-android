package com.segment.android.info.test;

import junit.framework.Assert;

import org.junit.Test;

import com.segment.android.info.Info;
import com.segment.android.info.SessionId;

import android.test.AndroidTestCase;

public class SessionIdTest extends AndroidTestCase {

	private Info<String> info = new SessionId();
	
	@Test
	public void testGet() {
		String sessionId = info.get(this.getContext());
		Assert.assertNotNull(sessionId);
	} 
	
	
}
