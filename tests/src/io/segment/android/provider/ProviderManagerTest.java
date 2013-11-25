package io.segment.android.provider;

import io.segment.android.Defaults;
import io.segment.android.cache.ISettingsLayer;
import io.segment.android.cache.SettingsCache;
import io.segment.android.cache.SettingsThread;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.Alias;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.models.Traits;
import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.test.BaseTest;
import io.segment.android.test.TestCases;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Test;

import android.content.Context;


public class ProviderManagerTest extends BaseTest {

	private Context context;
	private IRequester requester;
	private ISettingsLayer layer;
	
	@Override
	protected void setUp() {
		super.setUp();
		
		context = this.getContext();
		requester = new BasicRequester();
		layer = new SettingsThread(requester);
	}
	
	@Test
	public void testInitialization() {

		SettingsCache settingsCache = new SettingsCache(context, layer, Defaults.SETTINGS_CACHE_EXPIRY);
		ProviderManager providerManager = new ProviderManager(settingsCache);
		
		Assert.assertFalse(providerManager.isInitialized());
		
		providerManager.refresh();
		
		Assert.assertTrue(providerManager.isInitialized());
	}
	
	@Test
	public void testProviderSelection() {

		final String key = "Some Provider";

		// create a settings cache that will insert fake provider settings for this key 
		SettingsCache settingsCache = new SettingsCache(context, layer, Defaults.SETTINGS_CACHE_EXPIRY) {
			@Override
			public EasyJSONObject getSettings() {
				// get them directly from the server with blocking
				EasyJSONObject settings = requester.fetchSettings();
				if (settings != null)
					settings.putObject(key, new JSONObject());
				return settings;
			}
		};
		
		// make the sure the settings cache has nothing in it right now
		settingsCache.reset();
		
		ProviderManager providerManager = new ProviderManager(settingsCache);
		
		// removes all the providers
		providerManager.getProviders().clear();
		
		final AtomicInteger identifies = new AtomicInteger();
		final AtomicInteger tracks = new AtomicInteger();
		final AtomicInteger screens = new AtomicInteger();
		final AtomicInteger aliases = new AtomicInteger();
		
		Provider provider = new SimpleProvider() {
			@Override public void onCreate(Context context) { ready(); }
			@Override public String getKey() { return key; }
			@Override public void validate(EasyJSONObject settings) throws InvalidSettingsException {}
			@Override public void identify(Identify identify) { identifies.addAndGet(1); }
			@Override public void track(Track track) { tracks.addAndGet(1); }
			@Override public void screen(Screen screen) { screens.addAndGet(1); }
			@Override public void alias(Alias alias) { aliases.addAndGet(1); }
		};
		
		// add a simple adding provider
		providerManager.addProvider(provider);
		
		// get the settings from the server, which won't include this provider
		providerManager.refresh();
		
		// call the method that enables it
		providerManager.onCreate(context);
		
		//
		// Test the no specific context.providers added
		//
		
		providerManager.identify(TestCases.identify);
		Assert.assertEquals(1, identifies.get());
		
		providerManager.track(TestCases.track);
		Assert.assertEquals(1, tracks.get());

		providerManager.screen(TestCases.screen);
		Assert.assertEquals(1, screens.get());
		
		providerManager.alias(TestCases.alias);
		Assert.assertEquals(1, aliases.get());
		
		//
		// Assemble test values
		//

		String userId = TestCases.identify.getUserId();
		Traits traits = TestCases.identify.getTraits();
		Calendar timestamp = Calendar.getInstance();

		String event = TestCases.track.getEvent();
		EventProperties properties = TestCases.track.getProperties();

		String from = TestCases.alias.getFrom();
		String to = TestCases.alias.getTo();
		
		//
		// Test the context.providers.all = false setting default to false
		// 
		
		io.segment.android.models.Context allFalseContext = new io.segment.android.models.Context();
		allFalseContext.put("providers", new EasyJSONObject("all", false));
		
		providerManager.identify(new Identify(userId, traits, timestamp, allFalseContext));
		Assert.assertEquals(1, identifies.get());
		
		providerManager.track(new Track(userId, event, properties, timestamp, allFalseContext));
		Assert.assertEquals(1, tracks.get());
		
		providerManager.alias(new Alias(from, to, timestamp, allFalseContext));
		Assert.assertEquals(1, aliases.get());
		
		//
		// Test the context.providers[provider.key] = false turns it off
		//
		
		io.segment.android.models.Context providerFalseContext = new io.segment.android.models.Context();
		providerFalseContext.put("providers", new EasyJSONObject(key, false));
		
		providerManager.identify(new Identify(userId, traits, timestamp, providerFalseContext));
		Assert.assertEquals(1, identifies.get());
		
		providerManager.track(new Track(userId, event, properties, timestamp, providerFalseContext));
		Assert.assertEquals(1, tracks.get());
		
		providerManager.alias(new Alias(from, to, timestamp, providerFalseContext));
		Assert.assertEquals(1, aliases.get());
		

		//
		// Test the context.providers[provider.key] = true, All=false keeps it on
		//
		
		io.segment.android.models.Context providerTrueContext = new io.segment.android.models.Context();
		providerTrueContext.put("providers", new EasyJSONObject("All", false));
		providerTrueContext.put("providers", new EasyJSONObject(key, true));
		
		providerManager.identify(new Identify(userId, traits, timestamp, providerTrueContext));
		Assert.assertEquals(2, identifies.get());
		
		providerManager.track(new Track(userId, event, properties, timestamp, providerTrueContext));
		Assert.assertEquals(2, tracks.get());
		
		providerManager.alias(new Alias(from, to, timestamp, providerTrueContext));
		Assert.assertEquals(2, aliases.get());
	}
	
}
