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
import io.segment.android.integrations.QuantcastIntegration;
import io.segment.android.integrations.TapstreamIntegration;
import io.segment.android.models.Alias;
import io.segment.android.models.BasePayload;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Group;
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
	private List<Integration> integrations;
	private boolean initialized;
	
	public IntegrationManager(SettingsCache settingsCache) {
		this.settingsCache = settingsCache;
		
		this.integrations = new LinkedList<Integration>();
		
		/**
		 * Add New integrations Here
		 */
		this.addIntegration(new AmplitudeIntegration());
		this.addIntegration(new BugsnagIntegration());
		this.addIntegration(new CountlyIntegration());
		this.addIntegration(new CrittercismIntegration());
		this.addIntegration(new FlurryIntegration());
		this.addIntegration(new GoogleAnalyticsIntegration());
		this.addIntegration(new LocalyticsIntegration());
		this.addIntegration(new MixpanelIntegration());
		this.addIntegration(new QuantcastIntegration());
		this.addIntegration(new TapstreamIntegration());
	}
	
	/**
	 * Adds an integration to the integration manager
	 * @param integration
	 */
	public void addIntegration(Integration integration) {
		if (TextUtils.isEmpty(integration.getKey())) {
			throw new IllegalArgumentException("integration #getKey() " + 
					"must return a non-null non-empty integration key.");
		}
		
		this.integrations.add(integration);
	}

	/**
	 * Will run refresh if the integration manager hasn't yet
	 * been initialized.
	 * @return Returns whether the integration manager has been initialized.
	 */
	private boolean ensureInitialized() {
		if (!initialized) refresh();
		if (!initialized) {
			// we still haven't gotten any settings
			Log.i(TAG, "Integration manager waiting to be initialized ..");
		}
		return initialized;
	}
	
	public void refresh() {
		
		EasyJSONObject allSettings = settingsCache.getSettings();
		
		if (allSettings != null) {
			// we managed to get the settings
			
			for (Integration integration : integrations) {
				// iterate through all of the integrations we enabe
				String integrationKey = integration.getKey();
				if (allSettings.has(integrationKey)) {
					// the settings has info for this integration
					// initialize the integration with those settings
					EasyJSONObject settings = new EasyJSONObject(allSettings.getObject(integrationKey));
					
					Log.i(TAG, String.format("Downloaded settings for integration %s: %s", 
							integration.getKey(), settings.toString()));
					
					try {
						integration.initialize(settings);
						// enable the integration
						integration.enable();
						
						Log.i(TAG, String.format("Initialized and enabled integration %s", 
								integration.getKey()));
						
					} catch (InvalidSettingsException e) {
						
						Log.w(TAG, String.format("integration %s couldn't be initialized: %s", 
								integration.getKey(), e.getMessage()));
					}
					
				} else if (integration.getState().ge(IntegrationState.ENABLED)) {
					// if the setting was previously enabled but is no longer
					// in the settings, that means its been disabled
					integration.disable();
				} else {
					// settings don't mention this integration and its not enabled
					// so do nothing here
				}
			}
			
			// the integration manager has been initialized
			initialized = true;
		
			Log.i(TAG, "Initialized the Segment.io integration manager.");
			
		} else {
			Log.i(TAG, "Async settings aren't fetched yet, waiting ..");
		}
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	public List<Integration> getIntegrations() {
		return integrations;
	}

	public SettingsCache getSettingsCache() {
		return settingsCache;
	}
	
	public void setSettingsCache(SettingsCache settingsCache) {
		this.settingsCache = settingsCache;
	}
	
	/**
	 * A integration operation function
	 */
	private interface IntegrationOperation {
		public void run(Integration integration);
	}
	
	/**
	 * Run an operation on all the integrations
	 * @param name Name of the operation
	 * @param minimumState The minimum state that the integration has to be in before running the operation
	 * @param operation The actual operation to run on the integration
	 */
	private void runOperation(String name, IntegrationState minimumState, IntegrationOperation operation) {
		
		// time the operation
		Stopwatch createOp = new Stopwatch("[All integrations] " + name);

		// make sure that the integration manager has settings from the server first
		if (ensureInitialized()) {
			
			for (Integration integration : this.integrations) {
				// if the integration is at least in the minimum state 
				if (integration.getState().ge(minimumState)) {
					
					// time this integration's operation
					Stopwatch integrationOp = new Stopwatch("[" + integration.getKey() + "] " + name);
					
					operation.run(integration);
					
					integrationOp.end();
				}
			}
		}
		
		createOp.end();
	}
	

	public void toggleOptOut(final boolean optedOut) {
		runOperation("optOut", IntegrationState.INITIALIZED, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				integration.toggleOptOut(optedOut);
			}
		});
	}
	
	public void checkPermissions (Context context) {
		for (Integration integration : this.integrations) {
			integration.checkPermission(context);
		}
	}
	
	@Override
	public void onCreate(final Context context) {
		checkPermissions(context);
		
		runOperation("onCreate", IntegrationState.INITIALIZED, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				integration.onCreate(context);
			}
		});
	}

	@Override
	public void onActivityStart(final Activity activity) {
		
		runOperation("onActivityStart", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				integration.onActivityStart(activity);
			}
		});
	}

	@Override
	public void onActivityResume(final Activity activity) {
		
		runOperation("onActivityResume", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				integration.onActivityResume(activity);
			}
		});
	}

	@Override
	public void onActivityPause(final Activity activity) {
		
		runOperation("onActivityPause", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				integration.onActivityPause(activity);
			}
		});
	}
	
	@Override
	public void onActivityStop(final Activity activity) {
		
		runOperation("onActivityStop", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				integration.onActivityStop(activity);
			}
		});
	}

	/**
	 * Determines if the integration is enabled for this action
	 * @param integration integration
	 * @param action The action being processed
	 * @return
	 */
	private boolean isintegrationEnabled(Integration integration, BasePayload action) {
		
		boolean enabled = true;
		
		io.segment.android.models.Context context = action.getContext();
		
		String chooseIntegrationsKey = null;
		if (context != null) {
			if (context.has("integrations")) chooseIntegrationsKey = "integrations";
			else if (context.has("providers")) chooseIntegrationsKey = "providers";
		}
		
		if (chooseIntegrationsKey != null) {
			EasyJSONObject integrations = new EasyJSONObject(context.getObject(chooseIntegrationsKey));

			String key = integration.getKey();
			
			if (integrations.has("all")) enabled = integrations.getBoolean("all", true);
			if (integrations.has("All")) enabled = integrations.getBoolean("All", true);
	
			if (integrations.has(key)) enabled = integrations.getBoolean(key, true);
		}
		
		return enabled;
	}
	
	@Override
	public void identify(final Identify identify) {
		
		runOperation("Identify", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				
				if (isintegrationEnabled(integration, identify))
					integration.identify(identify);
			}
		});
	}

	@Override
	public void group(final Group group) {
		
		runOperation("Group", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				
				if (isintegrationEnabled(integration, group))
					integration.group(group);
			}
		});
	}
	
	@Override
	public void track(final Track track) {

		runOperation("Track", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				
				if (isintegrationEnabled(integration, track))
					integration.track(track);
			}
		});
	}
	
	@Override
	public void screen(final Screen screen) {

		runOperation("Screen", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				
				if (isintegrationEnabled(integration, screen))
					integration.screen(screen);
			}
		});
	}

	@Override
	public void alias(final Alias alias) {

		runOperation("Alias", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				
				if (isintegrationEnabled(integration, alias))
					integration.alias(alias);
			}
		});
	}

	public void reset() {

		runOperation("Reset", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				
				integration.reset();
			}
		});
	}
	
	public void flush() {

		runOperation("Flush", IntegrationState.READY, new IntegrationOperation () {
			
			@Override
			public void run(Integration integration) {
				
				integration.flush();
			}
		});
	}

}
