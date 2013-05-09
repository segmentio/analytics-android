package io.segment.android.provider;

import io.segment.android.Defaults;
import io.segment.android.cache.SettingsCache;
import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.test.BaseTest;
import junit.framework.Assert;

import org.junit.Test;

import android.content.Context;

public class ProviderManagerTest extends BaseTest {

	private ProviderManager providerManager;
	
	@Override
	protected void setUp() {
		super.setUp();
		
		Context context = this.getContext();
		IRequester requester = new BasicRequester();
		
		SettingsCache settingsCache = new SettingsCache(context, requester, Defaults.SETTINGS_CACHE_EXPIRY);
		providerManager = new ProviderManager(settingsCache);
	}
	
	@Test
	public void testInitialization() {
		
		Assert.assertFalse(providerManager.isInitialized());
		
		providerManager.refresh();
		
		Assert.assertTrue(providerManager.isInitialized());
	}
	
}
