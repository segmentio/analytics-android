package io.segment.android.providers;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Identify;
import io.segment.android.models.Traits;
import io.segment.android.provider.SimpleProvider;

import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.bugsnag.android.Bugsnag;

public class BugsnagProvider extends SimpleProvider {

	private static class SettingKey { 

		private static final String API_KEY = "apiKey";
		
		private static final String USE_SSL = "useSsl";
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
		
		Bugsnag.setUserId(userId);
		
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
