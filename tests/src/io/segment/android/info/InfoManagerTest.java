package io.segment.android.info;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import android.test.AndroidTestCase;

public class InfoManagerTest extends AndroidTestCase {

	private InfoManager manager = new InfoManager();
	
	@Test
	public void testGet() {
		JSONObject object = manager.build(this.getContext());
		Assert.assertTrue(object.length() > 0);
	} 
	
}
