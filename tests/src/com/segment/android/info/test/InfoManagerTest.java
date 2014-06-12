package com.segment.android.info.test;


import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.segment.android.Config;
import com.segment.android.info.InfoManager;

import android.test.AndroidTestCase;

public class InfoManagerTest extends AndroidTestCase {
	
	@Test
	public void testGet() {
		InfoManager manager = new InfoManager(new Config());
		JSONObject object = manager.build(this.getContext());
		Assert.assertTrue(object.length() > 0);
	} 
	
}
