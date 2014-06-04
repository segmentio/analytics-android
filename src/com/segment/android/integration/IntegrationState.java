package com.segment.android.integration;

/**
 * Represents the life-cycle stages of a provider.
 *
 */
public enum IntegrationState {

	/**
	 * The initial starting state o the provider, when it hasn't been initialized with settings from the server.
	 */
	NOT_INITIALIZED(0),
	/**
	 * The state at which settings have been provided, but they didn't have enough information to initialize the provider.
	 */
	INVALID(1),
	/**
	 * The state at which settings were provided and the provider is ready to be enabled.
	 */
	INITIALIZED(2),
	/**
	 * The state at which the provider has been explicitly disabled by the server.
	 */
	DISABLED(3),
	/**
	 * The state at which the provider has been initialized, and enabled by the the library. 
	 */
	ENABLED(4),
	/**
	 * The state at which the provider says that its ready to start processing data.
	 */
	READY(5);
	
	private final int value;
	
	private IntegrationState(int value) { 
		this.value = value; 
	}
  
	public int getValue() {
		return this.value;
	}
	
	/**
	 * Returns whether the current state is greater or equal to the other state provided.
	 * @param other
	 * @return
	 */
	public boolean ge(IntegrationState other) {
		return this.value >= other.value;
	}
}
