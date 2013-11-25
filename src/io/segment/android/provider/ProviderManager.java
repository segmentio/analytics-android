package io.segment.android.provider;

import io.segment.android.Constants;
import io.segment.android.cache.SettingsCache;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.Alias;
import io.segment.android.models.BasePayload;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.providers.AmplitudeProvider;
import io.segment.android.providers.BugsnagProvider;
import io.segment.android.providers.CountlyProvider;
import io.segment.android.providers.CrittercismProvider;
import io.segment.android.providers.FlurryProvider;
import io.segment.android.providers.GoogleAnalyticsProvider;
import io.segment.android.providers.LocalyticsProvider;
import io.segment.android.providers.MixpanelProvider;
import io.segment.android.providers.OmnitureProvider;
import io.segment.android.providers.QuantcastProvider;
import io.segment.android.providers.TapstreamProvider;
import io.segment.android.stats.Stopwatch;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class ProviderManager implements IProvider {

	private static final String TAG = Constants.TAG; 
	
	private SettingsCache settingsCache;
	private List<Provider> providers;
	private boolean initialized;
	
	public ProviderManager(SettingsCache settingsCache) {
		this.settingsCache = settingsCache;
		
		this.providers = new LinkedList<Provider>();
		
		/**
		 * Add New Providers Here
		 */
		this.addProvider(new AmplitudeProvider());
		this.addProvider(new BugsnagProvider());
		this.addProvider(new CountlyProvider());
		this.addProvider(new CrittercismProvider());
		this.addProvider(new FlurryProvider());
		this.addProvider(new GoogleAnalyticsProvider());
		this.addProvider(new LocalyticsProvider());
		this.addProvider(new MixpanelProvider());
		this.addProvider(new OmnitureProvider());
		this.addProvider(new QuantcastProvider());
		this.addProvider(new TapstreamProvider());
	}
	
	/**
	 * Adds a provider to the analytics manager
	 * @param provider
	 */
	public void addProvider(Provider provider) {
		if (TextUtils.isEmpty(provider.getKey())) {
			throw new IllegalArgumentException("Provider #getKey() " + 
					"must return a non-null non-empty provider key.");
		}
		
		this.providers.add(provider);
	}

	/**
	 * Will run refresh if the provider manager hasn't yet
	 * been initialized.
	 * @return Returns whether the provider manager has been initialized.
	 */
	private boolean ensureInitialized() {
		if (!initialized) refresh();
		if (!initialized) {
			// we still haven't gotten any settings
			Log.w(TAG, "The Segment.io provider manager has not yet been able to receive integration settings.");
		}
		return initialized;
	}
	
	public void refresh() {
		
		Log.d(TAG, "Refreshing provider manager ...");
		
		EasyJSONObject allSettings = settingsCache.getSettings();
		
		if (allSettings != null) {
			// we managed to get the settings
			
			for (Provider provider : providers) {
				// iterate through all of the providers we enabe
				String providerKey = provider.getKey();
				if (allSettings.has(providerKey)) {
					// the settings has info for this provider
					// initialize the provider with those settings
					EasyJSONObject settings = new EasyJSONObject(allSettings.getObject(providerKey));
					
					try {
						provider.initialize(settings);
						// enable the provider
						provider.enable();
						
					} catch (InvalidSettingsException e) {
						
						Log.w(TAG, String.format("Provider %s couldn't be initialized: %s", 
								provider.getKey(), e.getMessage()));
					}
					
				} else if (provider.getState().ge(ProviderState.ENABLED)) {
					// if the setting was previously enabled but is no longer
					// in the settings, that means its been disabled
					provider.disable();
				} else {
					// settings don't mention this provider and its not enabled
					// so do nothing here
				}
			}
			
			// the provider manager has been initialized
			initialized = true;
		
			Log.i(TAG, "Initialized the Segment.io provider manager.");
			
		} else {
			Log.i(TAG, "Async settings aren't fetched yet, waiting ..");
		}
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	public List<Provider> getProviders() {
		return providers;
	}

	public SettingsCache getSettingsCache() {
		return settingsCache;
	}
	
	public void setSettingsCache(SettingsCache settingsCache) {
		this.settingsCache = settingsCache;
	}
	
	/**
	 * A provider operation function
	 */
	private interface ProviderOperation {
		public void run(Provider provider);
	}
	
	/**
	 * Run an operation on all the providers
	 * @param name Name of the operation
	 * @param minimumState The minimum state that the provider has to be in before running the operation
	 * @param operation The actual operation to run on the provider
	 */
	private void runOperation(String name, ProviderState minimumState, ProviderOperation operation) {
		
		// time the operation
		Stopwatch createOp = new Stopwatch("[All Providers] " + name);

		// make sure that the provider manager has settings from the server first
		if (ensureInitialized()) {
			
			for (Provider provider : this.providers) {
				// if the provider is at least in the minimum state 
				if (provider.getState().ge(minimumState)) {
					
					// time this provider's operation
					Stopwatch providerOp = new Stopwatch("[" + provider.getKey() + "] " + name);
					
					operation.run(provider);
					
					providerOp.end();
				}
			}
		}
		
		createOp.end();
	}
	

	public void toggleOptOut(final boolean optedOut) {
		runOperation("optOut", ProviderState.INITIALIZED, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				provider.toggleOptOut(optedOut);
			}
		});
	}
	
	public void checkPermissions (Context context) {
		for (Provider provider : this.providers) {
			provider.checkPermission(context);
		}
	}
	
	@Override
	public void onCreate(final Context context) {
		checkPermissions(context);
		
		runOperation("onCreate", ProviderState.INITIALIZED, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				provider.onCreate(context);
			}
		});
	}

	@Override
	public void onActivityStart(final Activity activity) {
		
		runOperation("onActivityStart", ProviderState.INITIALIZED, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				provider.onActivityStart(activity);
			}
		});
	}

	@Override
	public void onActivityStop(final Activity activity) {
		
		runOperation("onActivityStop", ProviderState.INITIALIZED, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				provider.onActivityStop(activity);
			}
		});
	}

	/**
	 * Determines if the provider is enabled for this action
	 * @param provider Provider
	 * @param action The action being processed
	 * @return
	 */
	private boolean isProviderEnabled(Provider provider, BasePayload action) {
		
		boolean enabled = true;
		
		io.segment.android.models.Context context = action.getContext();
		if (context != null && context.has("providers")) {
			EasyJSONObject providers = new EasyJSONObject(context.getObject("providers"));

			String key = provider.getKey();
			
			if (providers.has("all")) enabled = providers.getBoolean("all", true);
			if (providers.has("All")) enabled = providers.getBoolean("All", true);
	
			if (providers.has(key)) enabled = providers.getBoolean(key, true);
		}
		
		return enabled;
	}
	
	
	@Override
	public void identify(final Identify identify) {
		
		runOperation("Identify", ProviderState.READY, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				
				if (isProviderEnabled(provider, identify))
					provider.identify(identify);
			}
		});
	}

	@Override
	public void track(final Track track) {

		runOperation("Track", ProviderState.READY, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				
				if (isProviderEnabled(provider, track))
					provider.track(track);
			}
		});
	}
	
	@Override
	public void screen(final Screen screen) {

		runOperation("Screen", ProviderState.READY, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				
				if (isProviderEnabled(provider, screen))
					provider.screen(screen);
			}
		});
	}

	@Override
	public void alias(final Alias alias) {

		runOperation("Alias", ProviderState.READY, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				
				if (isProviderEnabled(provider, alias))
					provider.alias(alias);
			}
		});
	}

	public void reset() {

		runOperation("Reset", ProviderState.READY, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				
				provider.reset();
			}
		});
	}
	
	public void flush() {

		runOperation("Flush", ProviderState.READY, new ProviderOperation () {
			
			@Override
			public void run(Provider provider) {
				
				provider.flush();
			}
		});
	}

}
