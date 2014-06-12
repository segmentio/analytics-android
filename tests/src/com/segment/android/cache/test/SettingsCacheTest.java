package com.segment.android.cache.test;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.segment.android.cache.SettingsCache;
import com.segment.android.cache.SettingsThread;
import com.segment.android.request.BasicRequester;
import com.segment.android.request.IRequester;
import com.segment.android.test.BaseTest;

import android.content.Context;

public class SettingsCacheTest extends BaseTest {

	private static SettingsCache cache;
	private static IRequester requester;
	private static int cacheForMs;
	
	@BeforeClass
	protected void setUp() {
		super.setUp();
		
		Context context = this.getContext();
		requester = new BasicRequester();
		cacheForMs = 1000;
		SettingsThread thread = new SettingsThread(requester);
		thread.start();
		cache = new SettingsCache(context, thread, cacheForMs);
		// resets the cache by removing the settings
		cache.reset();
	}
	
	@Test
	public void loadTest() {
		
		int reloads = cache.getReloads();
		
		Assert.assertEquals(reloads, cache.getReloads());
		Assert.assertNotNull(cache.get());
		Assert.assertEquals(reloads + 1, cache.getReloads());
		Assert.assertNotNull(cache.getSettings());
		Assert.assertEquals(reloads + 1, cache.getReloads());
		
		Assert.assertNotNull(cache.get());
		Assert.assertEquals(reloads + 1, cache.getReloads());

		Assert.assertNotNull(cache.getSettings());		
		Assert.assertEquals(reloads + 1, cache.getReloads());

	}
	
	
	/* TODO:  Test needs to be fixed
	@Test
	public void testRefreshTest() {
		
		int reloads = cache.getReloads();
		
		Assert.assertEquals(reloads, cache.getReloads());
		Assert.assertNotNull(cache.get());
		Assert.assertEquals(reloads + 1, cache.getReloads());
		
		try {
			Thread.sleep(cacheForMs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Assert.assertEquals(reloads + 1, cache.getReloads());
		Assert.assertNotNull(cache.getSettings());
		Assert.assertEquals(reloads + 2, cache.getReloads());
	}
	*/
	
}
