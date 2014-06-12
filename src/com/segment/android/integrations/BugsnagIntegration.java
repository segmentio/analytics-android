package com.segment.android.integrations;


import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.bugsnag.android.Bugsnag;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Traits;

public class BugsnagIntegration extends SimpleIntegration {

	private static class SettingKey { 

		private static final String API_KEY = "apiKey";
		
		private static final String USE_SSL = "useSSL";
	}

	@Override
	public String getKey() {
		return "Bugsnag";
	}

	@Override
	public void validate(EasyJSONObject settings)
			throws InvalidSettingsException {
	

		if (TextUtils.isEmpty(settings.getString(SettingKey.API_KEY))) {
			throw new InvalidSettingsException(SettingKey.API_KEY, "Bugsnag requires the setting apiKey.");
		}
	}
	
	@Override
	public void onCreate(Context context) {
		
		EasyJSONObject settings = this.getSettings();
		String apiKey = settings.getString(SettingKey.API_KEY);
		
		boolean useSsl = settings.getBoolean(SettingKey.USE_SSL, false);

		Bugsnag.setUseSSL(useSsl);
		
		Bugsnag.register(context, apiKey);
		
		ready();
	}
	
	@Override
	public void onActivityStart(Activity activity) { 
		Bugsnag.setContext(activity.getLocalClassName());
	}
	
	@Override
	public void identify(Identify identify) {
		String userId = identify.getUserId();
		Traits traits = identify.getTraits();
		
		String email = traits.getString("email", "user@gmail.com");
		String name = traits.getString("name", "User Name");
		
		Bugsnag.setUser(userId, email, name);
		
		if (traits != null) {
			@SuppressWarnings("unchecked")
			Iterator<String> keys = traits.keys();
			while(keys.hasNext()) {
				String key = keys.next();
				Object value = traits.get(key);
				Bugsnag.addToTab("User", key, value);	
			}
		}
	}
	
}
