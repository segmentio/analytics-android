package io.segment.android.provider;

import io.segment.android.Constants;
import io.segment.android.cache.SettingsCache;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.Alias;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Identify;
import io.segment.android.models.Track;
import io.segment.android.providers.AmplitudeProvider;
import io.segment.android.providers.BugsnagProvider;
import io.segment.android.providers.CountlyProvider;
import io.segment.android.providers.CrashlyticsProvider;
import io.segment.android.providers.CrittercismProvider;
import io.segment.android.providers.FlurryProvider;
import io.segment.android.providers.GoogleAnalyticsProvider;
import io.segment.android.providers.LocalyticsProvider;
import io.segment.android.providers.MixpanelProvider;

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
		this.addProvider(new CrashlyticsProvider());
		this.addProvider(new CrittercismProvider());
		this.addProvider(new FlurryProvider());
		this.addProvider(new GoogleAnalyticsProvider());
		this.addProvider(new LocalyticsProvider());
		this.addProvider(new MixpanelProvider());
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
			Log.w(TAG, "Failed to initialize Segment.io provider manager.");
		}
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	public List<Provider> getProviders() {
		return providers;
	}

	@Override
	public void onCreate(Context context) {
		
		if (ensureInitialized()) {
			for (Provider provider : this.providers) {
				if (provider.getState().ge(ProviderState.INITIALIZED)) {
					provider.onCreate(context);
				}
			}
		}
	}

	@Override
	public void onActivityStart(Activity activity) {
		
		if (ensureInitialized()) {
			for (Provider provider : this.providers) {
				if (provider.getState().ge(ProviderState.INITIALIZED)) {
					provider.onActivityStart(activity);
				}
			}
		}
	}

	@Override
	public void onActivityStop(Activity activity) {
		
		if (ensureInitialized()) {
			for (Provider provider : this.providers) {
				if (provider.getState().ge(ProviderState.INITIALIZED)) {
					provider.onActivityStop(activity);
				}
			}
		}
	}

	@Override
	public void identify(Identify identify) {

		if (ensureInitialized()) {
			for (Provider provider : this.providers) {
				if (provider.getState().ge(ProviderState.READY)) {
					provider.identify(identify);
				}
			}
		}
	}

	@Override
	public void track(Track track) {
		
		if (ensureInitialized()) {
			for (Provider provider : this.providers) {
				if (provider.getState().ge(ProviderState.READY)) {
					provider.track(track);
				}
			}
		}
	}

	@Override
	public void alias(Alias alias) {
		
		if (ensureInitialized()) {
			for (Provider provider : this.providers) {
				if (provider.getState().ge(ProviderState.READY)) {
					provider.alias(alias);
				}
			}
		}
	}
	
	public void flush() {
		
		if (ensureInitialized()) {
			for (Provider provider : this.providers) {
				if (provider.getState().ge(ProviderState.READY)) {
					provider.flush();
				}
			}
		}
	}
}
