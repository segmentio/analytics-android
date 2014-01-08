package io.segment.android.models;

import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Batch extends EasyJSONObject {

	private final static String WRITE_KEY = "writeKey";
	private final static String BATCH_KEY = "batch";
	private final static String CONTEXT_KEY = "context";
	private final static String REQUEST_TIMESTAMP_KEY = "requestTimestamp";
	
	public Batch(String writeKey, List<BasePayload> batch) {
		setWriteKey(writeKey);
		setBatch(batch);
	}
	
	public Context getContext() {
		JSONObject object = getObject(CONTEXT_KEY);
		if (object == null) return null;
		else return new Context(object);
	}

	public void setContext(Context context) {
		this.put(CONTEXT_KEY, context);
	}
	
	public String getWriteKey() {
		return this.optString(WRITE_KEY, null);
	}

	public void setWriteKey(String writeKey) {
		this.put(WRITE_KEY, writeKey);
	}

	public List<BasePayload> getBatch() {
		return this.<BasePayload>getArray(BATCH_KEY);
	}

	public void setBatch(List<BasePayload> batch) {
		this.put(BATCH_KEY, new JSONArray(batch));
	}

	public Calendar getRequestTimestamp() {
		return getCalendar(REQUEST_TIMESTAMP_KEY);
	}

	public void setRequestTimestamp(Calendar timestamp) {
		super.put(REQUEST_TIMESTAMP_KEY, timestamp);
	}
}