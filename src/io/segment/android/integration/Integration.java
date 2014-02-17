package io.segment.android.integration;

import io.segment.android.Logger;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.utils.AndroidUtils;
import android.content.Context;

public abstract class Integration implements IIntegration {
		
	private EasyJSONObject settings;
	private IntegrationState state = IntegrationState.NOT_INITIALIZED;
	protected boolean hasPermission = true;

	/**
	 * Resets the base provider to a state of NOT_INITIALIZED, and resets the settings
	 */
	public void reset() {
		settings = null;
		state = IntegrationState.NOT_INITIALIZED;
	}
	
	public void initialize(EasyJSONObject settings) throws InvalidSettingsException {
		if (this.settings != null) {
			if (!EasyJSONObject.equals(this.settings, settings)) { 
				Logger.w(String.format("Provider %s settings changed. %s => %s", getKey(), this.settings, settings));
			}
		}
		
		try {
			// try to validate the settings
			validate(settings);
			
			this.settings = settings;
			
			// if we get past validation, then its 
			changeState(IntegrationState.INITIALIZED, new IntegrationState[] {
				// if the provider hasn't been initialized yet, then allow it to be initialized
				IntegrationState.NOT_INITIALIZED,
				// if the provider was invalid previously, but is now valid, you can mark it as initialized
				IntegrationState.INVALID
			});
		} catch (InvalidSettingsException e) {
			
			// if we get past validation, then its 
			changeState(IntegrationState.INVALID, new IntegrationState[] {
				// if the provider hasn't been initialized yet, then its settings could be marked invalid
				IntegrationState.NOT_INITIALIZED
			});
			
			throw e;
		}
	}
	
	/**
	 * Checks whether this provider has permission in this applicatio
	 * @param context
	 * @return
	 */
	public boolean checkPermission(Context context) {
		String[] permissions = getRequiredPermissions();
		for (String permission : permissions) {
			if (!AndroidUtils.permissionGranted(context, permission)) {
				Logger.w(String.format("Provider %s requires permission %s but its not granted.", getKey(), permission));
				changeState(IntegrationState.INVALID, new IntegrationState[] {
					IntegrationState.NOT_INITIALIZED,
					IntegrationState.INITIALIZED
				});
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the required permissions for this provider to run.
	 * @return
	 */
	
	public abstract String[] getRequiredPermissions ();
	
	/**
	 * Enable this provider if its initialized or disabled.
	 * @return Whether or not this provider was enabled.
	 */
	public boolean enable() {
		return changeState(IntegrationState.ENABLED, new IntegrationState[] {
			// if the provider already has the settings, it can be enabled
			IntegrationState.INITIALIZED,
			// if the provider is disabled, it can be re-enabled
			IntegrationState.DISABLED,
			// if the provider is already enabled, this is a no-op
			IntegrationState.ENABLED
		});
	}

	/**
	 * Disable this provider.
	 * @return Whether or not this provider was disabled.
	 */
	public boolean disable() {
		return changeState(IntegrationState.DISABLED, new IntegrationState[] {
			// if the provider already has the settings, it can be enabled
			IntegrationState.INITIALIZED,
			// if the provider is disabled, it can be re-enabled
			IntegrationState.DISABLED,
			// if the provider is already enabled, this is a no-op
			IntegrationState.ENABLED,
			// if the provider is already "ready for data", this is a no-op
			IntegrationState.READY
		});
	}
	
	private boolean changeState(IntegrationState to, IntegrationState[] acceptedFromStates) {
		
		// if we're not actually changing state, just return early
		if (state == to) return true;
		
		boolean acceptedState = false;
		for (IntegrationState from : acceptedFromStates) {
			if (state == from) {
				acceptedState = true;
				break;
			}
		}
		
		if (acceptedState) {
			state = to;
			return true;
		} else {
			Logger.w("Provider " + getKey() + 
					" cant be " + to + " because its in state " + state + ".");
			return false;
		}
	}
	
	/**
	 * Called when the provider is ready to process data.
	 * @return Whether the provider was able to be marked as ready (initialized and enabled)
	 */
	public boolean ready() {
		return changeState(IntegrationState.READY, new IntegrationState[] {
			// if the provider is enabled, then it can be transitioned to send data
			IntegrationState.ENABLED
		});
	}
	
	/**
	 * Returns the state of this provider
	 * @return
	 */
	public IntegrationState getState() {
		return state;
	}
	
	/**
	 * Returns the settings for this provider.
	 * @return
	 */
	public EasyJSONObject getSettings() {
		return settings;
	}
	
	/**
	 * Returns the Segment.io key for this provider ("Mixpanel", "KISSMetrics", "Salesforce", etc ..)
	 * Check with friends@segment.io if you're not sure what the key is.
	 * @return
	 */
	public abstract String getKey();
	
	/**
	 * Validates that the provided settings are enough for this driver to perform its function.
	 * Will throw {@link InvalidSettingsException} if the settings are not enough. 
	 * @param settings The Segment.io provider settings
	 * @throws InvalidSettingsException An exception that says a field setting is invalid
	 */
	public abstract void validate(EasyJSONObject settings) throws InvalidSettingsException;
	
}
