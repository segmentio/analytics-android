package io.segment.android.provider;

import io.segment.android.Logger;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;

public abstract class Provider implements IProvider {
		
	private EasyJSONObject settings;
	private ProviderState state = ProviderState.NOT_INITIALIZED;

	/**
	 * Resets the base provider to a state of NOT_INITIALIZED, and resets the settings
	 */
	public void reset() {
		settings = null;
		state = ProviderState.NOT_INITIALIZED;
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
			changeState(ProviderState.INITIALIZED, new ProviderState[] {
				// if the provider hasn't been initialized yet, then allow it to be initialized
				ProviderState.NOT_INITIALIZED,
				// if the provider was invalid previously, but is now valid, you can mark it as initialized
				ProviderState.INVALID
			});
		} catch (InvalidSettingsException e) {
			
			// if we get past validation, then its 
			changeState(ProviderState.INVALID, new ProviderState[] {
				// if the provider hasn't been initialized yet, then its settings could be marked invalid
				ProviderState.NOT_INITIALIZED
			});
			
			throw e;
		}
	}
	
	/**
	 * Enable this provider if its initialized or disabled.
	 * @return Whether or not this provider was enabled.
	 */
	public boolean enable() {
		return changeState(ProviderState.ENABLED, new ProviderState[] {
			// if the provider already has the settings, it can be enabled
			ProviderState.INITIALIZED,
			// if the provider is disabled, it can be re-enabled
			ProviderState.DISABLED,
			// if the provider is already enabled, this is a no-op
			ProviderState.ENABLED,
			// if the provider is already "ready for data", this is a no-op
			ProviderState.READY
		});
	}

	/**
	 * Disable this provider.
	 * @return Whether or not this provider was disabled.
	 */
	public boolean disable() {
		return changeState(ProviderState.DISABLED, new ProviderState[] {
			// if the provider already has the settings, it can be enabled
			ProviderState.INITIALIZED,
			// if the provider is disabled, it can be re-enabled
			ProviderState.DISABLED,
			// if the provider is already enabled, this is a no-op
			ProviderState.ENABLED,
			// if the provider is already "ready for data", this is a no-op
			ProviderState.READY
		});
	}
	
	private boolean changeState(ProviderState to, ProviderState[] acceptedFromStates) {
		
		// if we're not actually changing state, just return early
		if (state == to) return true;
		
		boolean acceptedState = false;
		for (ProviderState from : acceptedFromStates) {
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
		return changeState(ProviderState.READY, new ProviderState[] {
			// if the provider is enabled, then it can be transitioned to send data
			ProviderState.ENABLED
		});
	}
	
	/**
	 * Returns the state of this provider
	 * @return
	 */
	public ProviderState getState() {
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
