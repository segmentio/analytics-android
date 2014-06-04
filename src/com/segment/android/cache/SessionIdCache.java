package com.segment.android.cache;

import com.segment.android.Constants;
import com.segment.android.info.SessionId;

import android.content.Context;

public class SessionIdCache extends SimpleStringCache {

	private Context context;
	private SessionId sessionIdLoader;
	
	public SessionIdCache(Context context) {
		super(context, Constants.SharedPreferences.SESSION_ID_KEY);
		
		this.context = context;
		sessionIdLoader = new SessionId();
	}

	@Override
	public String load() {
		return sessionIdLoader.get(context);
	}
	
}
