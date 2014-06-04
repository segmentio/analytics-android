package com.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

/**
 * Options allows you to specify a timestamp, 
 * an anonymousId, a context, or target integrations.
 */
public class Options extends EasyJSONObject {

	private static final String ANONYMOUS_ID_KEY = "anonymousId";
	private static final String TIMESTAMP_KEY = "timestamp";
	private static final String INTEGRATIONS_KEY = "integrations";
	private static final String CONTEXT_KEY = "context";

	public Options() {
		super();
		initialize();
	}

	public Options(JSONObject obj) {
		super(obj);
		initialize();
	}

	public Options(Object... kvs) {
		super(kvs);
		initialize();
	}
	
	private void initialize() {
		// ensure that integrations: {} and context: {} always exist in the json
		// in case the user decides to set it
		if (this.getIntegrations() == null) this.put(INTEGRATIONS_KEY, new EasyJSONObject());
		if (this.getContext() == null) this.put(CONTEXT_KEY, new Context());
		// we want to time stamp the events in case there's no connectivity, and the actions get sent
		// in the future
		if (this.getTimestamp() == null) this.put(TIMESTAMP_KEY, Calendar.getInstance());
	}

	/**
	 * Sets the cookie / anonymous Id of this visitor. Use the anonymous Id
	 * to associate the actions previously taken by the cookied but anonymous user.
	 * @param anonymousId
	 * @return This options object for chaining
	 */
	public Options setAnonymousId(String anonymousId) {
		this.put(ANONYMOUS_ID_KEY, anonymousId);
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
		this.put(TIMESTAMP_KEY, timestamp);
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
	public Options setIntegration(String key, boolean enabled) {
		this.getIntegrations().put(key, enabled);
		return this;
	}
	
	/**
	 * Sets the context of this analytics call. Context contains information about the environment
	 * such as the app name and version, the visitor's user agent, ip, etc ..
	 * @param context The context object
	 * @return This options object for chaining
	 */
	public Options setContext(Context context) {
		this.put(CONTEXT_KEY, context);
		return this;
	}
	
	public String getAnonymousId() {
		return this.getString(ANONYMOUS_ID_KEY);
	}
	
	public Calendar getTimestamp() {
		return this.getCalendar(TIMESTAMP_KEY);
	}

	public EasyJSONObject getIntegrations() {
		return (EasyJSONObject) this.getObject(INTEGRATIONS_KEY);
	}
	
	public Context getContext() {
		return (Context) this.getObject(CONTEXT_KEY);
	}
	
	@Override
	public Options put(String key, Object value) {
		super.putObject(key, value);
		return this;
	}
	
}