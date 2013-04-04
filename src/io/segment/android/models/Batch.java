package io.segment.android.models;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Batch extends EasyJSONObject {

	private final static String SECRET_KEY = "secret";
	private final static String BATCH_KEY = "batch";
	private final static String CONTEXT_KEY = "context";
	
	public Batch(String secret, List<BasePayload> batch) {
		setSecret(secret);
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
	
	public String getSecret() {
		return this.optString(SECRET_KEY, null);
	}

	public void setSecret(String secret) {
		this.put(SECRET_KEY, secret);
	}

	public List<BasePayload> getBatch() {
		return this.<BasePayload>getArray(BATCH_KEY);
	}

	public void setBatch(List<BasePayload> batch) {
		this.put(BATCH_KEY, new JSONArray(batch));
	}

}