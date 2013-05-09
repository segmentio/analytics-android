package io.segment.android.cache.test;

import io.segment.android.cache.SettingsCache;
import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.test.BaseTest;
import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

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
		cache = new SettingsCache(context, requester, cacheForMs);
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
	
}
