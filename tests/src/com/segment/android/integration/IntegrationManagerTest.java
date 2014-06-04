package com.segment.android.integration;


import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Test;

import com.segment.android.Defaults;
import com.segment.android.cache.ISettingsLayer;
import com.segment.android.cache.SettingsCache;
import com.segment.android.cache.SettingsThread;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.Integration;
import com.segment.android.integration.IntegrationManager;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.Alias;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;
import com.segment.android.request.BasicRequester;
import com.segment.android.request.IRequester;
import com.segment.android.test.BaseTest;
import com.segment.android.test.TestCases;

import android.content.Context;

public class IntegrationManagerTest extends BaseTest {

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
		IntegrationManager integrationManager = new IntegrationManager(settingsCache);
		
		Assert.assertFalse(integrationManager.isInitialized());
		
		integrationManager.refresh();
		
		Assert.assertTrue(integrationManager.isInitialized());
	}
	
	@Test
	public void testIntegrationSelection() {

		final String key = "Some Integration";

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
		
		IntegrationManager integrationManager = new IntegrationManager(settingsCache);
		
		// removes all the providers
		integrationManager.getIntegrations().clear();
		
		final AtomicInteger identifies = new AtomicInteger();
		final AtomicInteger tracks = new AtomicInteger();
		final AtomicInteger screens = new AtomicInteger();
		final AtomicInteger aliases = new AtomicInteger();
		
		Integration provider = new SimpleIntegration() {
			@Override public void onCreate(Context context) { ready(); }
			@Override public String getKey() { return key; }
			@Override public void validate(EasyJSONObject settings) throws InvalidSettingsException {}
			@Override public void identify(Identify identify) { identifies.addAndGet(1); }
			@Override public void track(Track track) { tracks.addAndGet(1); }
			@Override public void screen(Screen screen) { screens.addAndGet(1); }
			@Override public void alias(Alias alias) { aliases.addAndGet(1); }
		};
		
		// add a simple adding provider
		integrationManager.addIntegration(provider);
		
		// get the settings from the server, which won't include this provider
		integrationManager.refresh();
		
		// call the method that enables it
		integrationManager.onCreate(context);
		
		//
		// Test the no specific context.providers added
		//
		
		integrationManager.identify(TestCases.identify());
		Assert.assertEquals(1, identifies.get());
		
		integrationManager.track(TestCases.track());
		Assert.assertEquals(1, tracks.get());

		integrationManager.screen(TestCases.screen());
		Assert.assertEquals(1, screens.get());
		
		integrationManager.alias(TestCases.alias());
		Assert.assertEquals(1, aliases.get());
		
		//
		// Assemble test values
		//

		Identify identify = TestCases.identify();
		
		String sessionId = identify.getSessionId();
		String userId = identify.getUserId();
		Traits traits = identify.getTraits();
		Calendar timestamp = Calendar.getInstance();

		Track track = TestCases.track();
		
		String event = TestCases.track().getEvent();
		Props properties = track.getProperties();

		Alias alias = TestCases.alias();
		
		String from = alias.getFrom();
		String to = alias.getTo();
		
		//
		// Test the context.providers.all = false setting default to false
		// 
		
		com.segment.android.models.Context allFalseContext = new com.segment.android.models.Context();
		allFalseContext.put("providers", new EasyJSONObject("all", false));
		
		integrationManager.identify(new Identify(sessionId, userId, traits, timestamp, allFalseContext));
		Assert.assertEquals(1, identifies.get());
		
		integrationManager.track(new Track(sessionId, userId, event, properties, timestamp, allFalseContext));
		Assert.assertEquals(1, tracks.get());
		
		integrationManager.alias(new Alias(from, to, timestamp, allFalseContext));
		Assert.assertEquals(1, aliases.get());
		
		//
		// Test the context.providers[provider.key] = false turns it off
		//
		
		com.segment.android.models.Context providerFalseContext = new com.segment.android.models.Context();
		providerFalseContext.put("providers", new EasyJSONObject(key, false));
		
		integrationManager.identify(new Identify(sessionId, userId, traits, timestamp, providerFalseContext));
		Assert.assertEquals(1, identifies.get());
		
		integrationManager.track(new Track(sessionId, userId, event, properties, timestamp, providerFalseContext));
		Assert.assertEquals(1, tracks.get());
		
		integrationManager.alias(new Alias(from, to, timestamp, providerFalseContext));
		Assert.assertEquals(1, aliases.get());
		

		//
		// Test the context.providers[provider.key] = true, All=false keeps it on
		//
		
		com.segment.android.models.Context providerTrueContext = new com.segment.android.models.Context();
		providerTrueContext.put("providers", new EasyJSONObject("All", false));
		providerTrueContext.put("providers", new EasyJSONObject(key, true));
		
		integrationManager.identify(new Identify(sessionId, userId, traits, timestamp, providerTrueContext));
		Assert.assertEquals(2, identifies.get());
		
		integrationManager.track(new Track(sessionId, userId, event, properties, timestamp, providerTrueContext));
		Assert.assertEquals(2, tracks.get());
		
		integrationManager.alias(new Alias(from, to, timestamp, providerTrueContext));
		Assert.assertEquals(2, aliases.get());
	}
	
}
