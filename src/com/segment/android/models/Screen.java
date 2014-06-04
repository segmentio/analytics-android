package com.segment.android.models;

import org.json.JSONObject;

public class Screen extends BasePayload {
	
	public final static String TYPE = "screen";
	
	private final static String USER_ID_KEY = "userId";
	private static final String NAME_KEY = "name";
	private static final String CATEGORY_KEY = "category";
	private static final String PROPERTIES_KEY = "category";
	
	
	public Screen (JSONObject obj) {
		super(obj);
	}
	
	public Screen(String userId, 
				 String name, 
				 String category,
				 Props properties, 
				 Options options) {
		
		super(TYPE, options);
		setUserId(userId);
		setName(name);
		setCategory(category);
		setProperties(properties);
	}

	public String getUserId() {
		return this.optString(USER_ID_KEY, null);
	}

	public void setUserId(String userId) {
		this.put(USER_ID_KEY, userId);
	}
	
	public String getName() {
		return this.optString(NAME_KEY, null);
	}

	public void setName(String name) {
		this.put(NAME_KEY, name);
	}
	
	public String getCategory() {
		return this.optString(CATEGORY_KEY, null);
	}

	public void setCategory(String category) {
		this.put(CATEGORY_KEY, category);
	}

	public Props getProperties() {
		JSONObject object = getObject(PROPERTIES_KEY);
		if (object == null) return null;
		else return new Props(object);
	}

	public void setProperties(Props properties) {
		this.put(PROPERTIES_KEY, properties);
	}
}
