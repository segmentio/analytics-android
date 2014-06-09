package com.segment.android.models;

import java.util.Calendar;

/**
 * Options object that allows the specification of a timestamp, 
 * an anonymousId, a context, or target integrations.
 */
public class Options {

	private String anonymousId;
	private Calendar timestamp;
	private EasyJSONObject integrations;
	private Context context;
	
	public Options () {
		this.integrations = new Props();
		this.context = new Context();
		this.timestamp = Calendar.getInstance();
	}
	
	/**
	 * Sets the cookie / anonymous Id of this visitor. Use the anonymous Id
	 * to associate the actions previously taken by the cookied but anonymous user.
	 * @param anonymousId
	 * @return This options object for chaining
	 */
	public Options setAnonymousId(String anonymousId) {
		this.anonymousId = anonymousId;
		return this;
	}
	
	/**
	 * Sets whether this call will be sent to the target integration. Use "all" to select
	 * all integrations, like so: 
	 * 	.setIntegration("all", false)
	 * 	.setIntegration("Google Analytics", true)
	 * @param integration The integration name
	 * @param enabled True for enabled
	 * @return This options object for chaining
	 */
	public Options setIntegration(String integration, boolean enabled) {
		this.integrations.put(integration, enabled);
		return this;
	}
	
	/**
	 * Sets the context of this analytics call. Context contains information about the environment
	 * such as the app name and version, the visitor's user agent, ip, etc ..
	 * @param context The context object
	 * @return This options object for chaining
	 */
	public Options setContext(Context context) {
		this.context = context;
		return this;
	}
	
	/**
	 * Sets the timestamp of when an analytics call occurred. The timestamp is primarily used for 
	 * historical imports or if this event happened in the past. The timestamp is not required, 
	 * and if its not provided, our servers will timestamp the call as if it just happened.
	 * @param timestamp The time when this event happened
	 * @return This options object for chaining
	 */
	public Options setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
		return this;
	}
	
	public Calendar getTimestamp() {
		return timestamp;
	}
	
	public Context getContext() {
		return context;
	}
	
	public String getAnonymousId() {
		return anonymousId;
	}
	
	public EasyJSONObject getIntegrations() {
		return integrations;
	}
	
}