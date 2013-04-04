package io.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

public class BasePayload extends EasyJSONObject {
	
	private final static String USER_ID_KEY = "userId";
	private final static String TIMESTAMP_KEY = "timestamp";
	private final static String CONTEXT_KEY = "context";
	
	public BasePayload (JSONObject obj) {
		super(obj);
	}
	
	public BasePayload(String userId, 
					   Calendar timestamp, 
					   Context context) {

		setUserId(userId);
		setTimestamp(timestamp);
		setContext(context);
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

}