package com.segment.android.models;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;

public class Batch extends EasyJSONObject {

	private final static String WRITE_KEY = "writeKey";
	private final static String BATCH_KEY = "batch";
	private final static String MESSAGE_ID_KEY = "messageId";
	private final static String SENT_AT_KEY = "sentAt";
	
	public Batch(String writeKey, List<BasePayload> batch) {
		setWriteKey(writeKey);
		setBatch(batch);
		setMessageId(UUID.randomUUID().toString());
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

	public Calendar getSentAt() {
		return getCalendar(SENT_AT_KEY);
	}

	public void setSentAt(Calendar sentAt) {
		super.put(SENT_AT_KEY, sentAt);
	}
	
	public String getMessageId() {
		return this.getString(MESSAGE_ID_KEY);
	}

	public void setMessageId(String messageId) {
		super.put(MESSAGE_ID_KEY, messageId);
	}
	
}