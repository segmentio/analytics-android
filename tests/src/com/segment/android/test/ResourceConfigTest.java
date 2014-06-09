package com.segment.android.test;


import org.junit.Assert;
import org.junit.Test;

import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityTestCase;

import com.segment.android.Analytics;
import com.segment.android.Config;
import com.segment.android.ResourceConfig;

public class ResourceConfigTest extends ActivityTestCase {

	@Test
	public void testSecret() {
	    Context context = getInstrumentation().getContext();
		Resources resources = context.getResources();
		String writeKey = ResourceConfig.getWriteKey(context);
		Assert.assertEquals(resources.getString(R.string.analytics_secret), writeKey);
	}
	
	@Test
	public void testOptions() {
		Context context = getInstrumentation().getContext();
		Config options = ResourceConfig.getOptions(context);
		testOptions(context, options);
	}
	
	@Test
	public void testInitialization() {
	    Context context = getInstrumentation().getContext();
	    if (Analytics.isInitialized()) Analytics.close();
		Analytics.initialize(context);
		Config options = Analytics.getOptions();
		testOptions(context, options);
		Analytics.close();
	}
	
	private void testOptions(Context context, Config options) {
		
		Resources resources = context.getResources();
		
		Assert.assertEquals(resources.getInteger(R.integer.analytics_flush_after), options.getFlushAfter());
		Assert.assertEquals(resources.getInteger(R.integer.analytics_flush_at), options.getFlushAt());
		Assert.assertEquals(resources.getInteger(R.integer.analytics_max_queue_size), options.getMaxQueueSize());
		Assert.assertEquals(resources.getInteger(R.integer.analytics_settings_cache_expiry), options.getSettingsCacheExpiry());

		Assert.assertEquals(resources.getString(R.string.analytics_host), options.getHost());
		Assert.assertEquals(Boolean.parseBoolean(resources.getString(R.string.analytics_debug)), options.isDebug());
	}
	
}
