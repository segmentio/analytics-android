package io.segment.android.info.test;

import io.segment.android.Options;
import io.segment.android.info.InfoManager;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import android.test.AndroidTestCase;

public class InfoManagerTest extends AndroidTestCase {
	
	@Test
	public void testGet() {
		InfoManager manager = new InfoManager(new Options());
		JSONObject object = manager.build(this.getContext());
		Assert.assertTrue(object.length() > 0);
	} 
	
}
