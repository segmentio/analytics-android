package io.segment.android.models;

import java.util.Calendar;
import java.util.UUID;

import org.json.JSONObject;

public class BasePayload extends EasyJSONObject {
	
	private final static String USER_ID_KEY = "userId";
	private final static String TIMESTAMP_KEY = "timestamp";
	private final static String REQUEST_ID_KEY = "requestId";
	private final static String CONTEXT_KEY = "context";
	
	public BasePayload (JSONObject obj) {
		super(obj);
	}
	
	public BasePayload(String userId, 
					   Calendar timestamp, 
					   Context context) {

		// we want to time stamp the events in case they get 
		// batched in the future
		if (timestamp == null) timestamp = Calendar.getInstance();
		if (context == null) context = new Context();
		
		setUserId(userId);
		setTimestamp(timestamp);
		setContext(context);
		setRequestId(UUID.randomUUID().toString());
	}

	public String getUserId() {
		return this.optString(USER_ID_KEY, null);
	}

	public void setUserId(String userId) {
		this.put(USER_ID_KEY, userId);
	}

	public Calendar getTimestamp() {
		return getCalendar(TIMESTAMP_KEY);
	}

	public void setTimestamp(Calendar timestamp) {
		super.put(TIMESTAMP_KEY, timestamp);
	}

	public Context getContext() {
		JSONObject object = getObject(CONTEXT_KEY);
		if (object == null) return null;
		else return new Context(object);
	}

	public void setContext(Context context) {
		this.put(CONTEXT_KEY, context);
	}

	public String getRequestId() {
		return this.optString(REQUEST_ID_KEY, null);
	}

	public void setRequestId(String requestId) {
		this.put(REQUEST_ID_KEY, requestId);
	}

	/**
	 * Gets a description of this item.
	 * @return {String}
	 */
	public String toDescription() {
		return "[Item " + this.getRequestId() + "] " + 
				this.getString("action", "action");
	}
	
}