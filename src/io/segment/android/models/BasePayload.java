package io.segment.android.models;

import java.util.Date;

public class BasePayload extends EasyJSONObject {
	
	private final static String USER_ID_KEY = "userId";
	private final static String TIMESTAMP_KEY = "timestamp";
	private final static String CONTEXT_KEY = "context";
	
	
	public BasePayload(String userId, 
					   Date timestamp, 
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

	public Date getTimestamp() {
		return getDate(TIMESTAMP_KEY);
	}

	public void setTimestamp(Date timestamp) {
		super.put(TIMESTAMP_KEY, timestamp);
	}

	public Context getContext() {
		return this.<Context>getObject(CONTEXT_KEY);
	}

	public void setContext(Context context) {
		this.put(CONTEXT_KEY, context);
	}

}