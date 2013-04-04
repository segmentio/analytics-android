package io.segment.android.models;

import java.util.Date;

public class BasePayload extends EasyJsonObject {

	private String userId;
	private Context context;
	private Date timestamp;

	public BasePayload(String userId, 
					   Date timestamp, 
					   Context context) {

		this.userId = userId;
		this.timestamp = timestamp;
		this.context = context;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

}