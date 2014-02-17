package io.segment.android.integration;

import io.segment.android.Constants;
import io.segment.android.cache.SettingsCache;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.integrations.AmplitudeIntegration;
import io.segment.android.integrations.BugsnagIntegration;
import io.segment.android.integrations.CountlyIntegration;
import io.segment.android.integrations.CrittercismIntegration;
import io.segment.android.integrations.FlurryIntegration;
import io.segment.android.integrations.GoogleAnalyticsIntegration;
import io.segment.android.integrations.LocalyticsIntegration;
import io.segment.android.integrations.MixpanelIntegration;
import io.segment.android.integrations.OmnitureIntegration;
import io.segment.android.integrations.QuantcastIntegration;
import io.segment.android.integrations.TapstreamIntegration;
import io.segment.android.models.Alias;
import io.segment.android.models.BasePayload;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.stats.Stopwatch;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class IntegrationManager implements IIntegration {

	private static final String TAG = Constants.TAG; 
	
	private SettingsCache settingsCache;
	private List<Integration> providers;
	private boolean initialized;
	
	public IntegrationManager(SettingsCache settingsCache) {
		this.settingsCache = settingsCache;
		
		this.providers = new LinkedList<Integration>();
		
		/**
		 * Add New Providers Here
		 */
		this.addProvider(new AmplitudeIntegration());
		this.addProvider(new BugsnagIntegration());
		this.addProvider(new CountlyIntegration());
		this.addProvider(new CrittercismIntegration());
		this.addProvider(new FlurryIntegration());
		this.addProvider(new GoogleAnalyticsIntegration());
		this.addProvider(new LocalyticsIntegration());
		this.addProvider(new MixpanelIntegration());
		this.addProvider(new OmnitureIntegration());
		this.addProvider(new QuantcastIntegration());
		this.addProvider(new TapstreamIntegration());
	}
	
	/**
	 * Adds a provider to the analytics manager
	 * @param provider
	 */
	public void addProvider(Integration provider) {
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
			Log.i(TAG, "Provider manager waiting to be initialized ..");
		}
		return initialized;
	}
	
	public void refresh() {
		
		Log.d(TAG, "Refreshing provider manager ...");
		
		EasyJSONObject allSettings = settingsCache.getSettings();
		
		if (allSettings != null) {
			// we managed to get the settings
			
			for (Integration provider : providers) {
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
					
				} else if (provider.getState().ge(IntegrationState.ENABLED)) {
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
	
	public List<Integration> getProviders() {
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
		public void run(Integration provider);
	}
	
	/**
	 * Run an operation on all the providers
	 * @param name Name of the operation
	 * @param minimumState The minimum state that the provider has to be in before running the operation
	 * @param operation The actual operation to run on the provider
	 */
	private void runOperation(String name, IntegrationState minimumState, ProviderOperation operation) {
		
		// time the operation
		Stopwatch createOp = new Stopwatch("[All Providers] " + name);

		// make sure that the provider manager has settings from the server first
		if (ensureInitialized()) {
			
			for (Integration provider : this.providers) {
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
		runOperation("optOut", IntegrationState.INITIALIZED, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				provider.toggleOptOut(optedOut);
			}
		});
	}
	
	public void checkPermissions (Context context) {
		for (Integration provider : this.providers) {
			provider.checkPermission(context);
		}
	}
	
	@Override
	public void onCreate(final Context context) {
		checkPermissions(context);
		
		runOperation("onCreate", IntegrationState.INITIALIZED, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				provider.onCreate(context);
			}
		});
	}

	@Override
	public void onActivityStart(final Activity activity) {
		
		runOperation("onActivityStart", IntegrationState.INITIALIZED, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				provider.onActivityStart(activity);
			}
		});
	}

	@Override
	public void onActivityStop(final Activity activity) {
		
		runOperation("onActivityStop", IntegrationState.INITIALIZED, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
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
	private boolean isProviderEnabled(Integration provider, BasePayload action) {
		
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
		
		runOperation("Identify", IntegrationState.READY, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				
				if (isProviderEnabled(provider, identify))
					provider.identify(identify);
			}
		});
	}

	@Override
	public void track(final Track track) {

		runOperation("Track", IntegrationState.READY, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				
				if (isProviderEnabled(provider, track))
					provider.track(track);
			}
		});
	}
	
	@Override
	public void screen(final Screen screen) {

		runOperation("Screen", IntegrationState.READY, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				
				if (isProviderEnabled(provider, screen))
					provider.screen(screen);
			}
		});
	}

	@Override
	public void alias(final Alias alias) {

		runOperation("Alias", IntegrationState.READY, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				
				if (isProviderEnabled(provider, alias))
					provider.alias(alias);
			}
		});
	}

	public void reset() {

		runOperation("Reset", IntegrationState.READY, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				
				provider.reset();
			}
		});
	}
	
	public void flush() {

		runOperation("Flush", IntegrationState.READY, new ProviderOperation () {
			
			@Override
			public void run(Integration provider) {
				
				provider.flush();
			}
		});
	}

}
