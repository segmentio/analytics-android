package io.segment.android.models;

import java.util.List;

public class Batch {

	private String secret;
	private List<BasePayload> batch;
	
	/**
	 * Additional context set for every object in this batch
	 */
	private Context context;

	public Batch(String secret, List<BasePayload> batch) {
		this.secret = secret;
		this.batch = batch;
	}

	public void setContext(Context context) {
		this.context = context;
	}
	
	public Context getContext() {
		return context;
	}
	
	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public List<BasePayload> getBatch() {
		return batch;
	}

	public void setBatch(List<BasePayload> batch) {
		this.batch = batch;
	}

}