package com.segment.android.models;


import org.json.JSONObject;

import com.segment.android.Analytics;


public class Context extends EasyJSONObject {

	private static final String IP_KEY = "ip";
	private static final String LIBRARY_KEY = "library";
	private static final String LIBRARY_VERSION_KEY = "libraryVersion";
	private static final String PROVIDERS_KEY = "providers";

	public Context() {
		super();
		addLibraryContext();
	}

	public Context(JSONObject obj) {
		super(obj);
		addLibraryContext();
	}

	public Context(Object... kvs) {
		super(kvs);
		addLibraryContext();
	}

	private void addLibraryContext() {
		this.put(LIBRARY_KEY, "analytics-android");
		this.put(LIBRARY_VERSION_KEY, Analytics.VERSION);
	}

	public Context setIp(String ip) {
		this.put(IP_KEY, ip);
		return this;
	}

	public Context setProviders(Providers providers) {
		this.put(PROVIDERS_KEY, providers);
		return this;
	}

	public String getIp() {
		return (String) this.get(IP_KEY);
	}
	
	@Override
	public Context put(String key, Object value) {
		super.putObject(key, value);
		return this;
	}
	
	/**
	 * Returns whether the context.provider[key] === true
	 * Used for business integrations like Omniture where
	 * you don't want to send any track to the server, but
	 * only specifically ones that were selected.
	 * @param providerKey Provider Key
	 */
	public boolean isProviderStrictlyEnabled(String providerKey) {
		boolean enabled = false;
		
		if (this.has(PROVIDERS_KEY)) {
			EasyJSONObject object = new EasyJSONObject(this.getObject(PROVIDERS_KEY));
			if (object.has(providerKey)) {
				enabled = object.getBoolean(providerKey, false);
			}
		}
		
		return enabled;
	}
}