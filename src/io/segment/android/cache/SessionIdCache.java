package io.segment.android.cache;

import io.segment.android.Constants;
import io.segment.android.info.SessionId;
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
