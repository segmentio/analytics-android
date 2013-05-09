package io.segment.android.providers;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Identify;
import io.segment.android.models.Traits;
import io.segment.android.provider.SimpleProvider;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

public class CrashlyticsProvider extends SimpleProvider{

	private static class SettingKey { 
		
		private static final String API_KEY = "apiKey";
	}
	
	@Override
	public String getKey() {
		return "crashlytics";
	}

	@Override
	public void validate(EasyJSONObject settings)
			throws InvalidSettingsException {

		if (TextUtils.isEmpty(settings.getString(SettingKey.API_KEY))) {
			throw new InvalidSettingsException(SettingKey.API_KEY, "Crashlytics API key is required.");
		}
	}
	
	@Override
	public void onCreate(Context context) {

		EasyJSONObject settings = this.getSettings();
		String apiKey = settings.getString(SettingKey.API_KEY);
		
		Crashlytics.setApplicationInstallationIdentifier(apiKey);
		
		Crashlytics.start(context);
		
		ready();
	}
	
	@Override
	public void onActivityStart(Activity activity) {
		Crashlytics.start(activity);
	}
	
	@Override
	public void identify(Identify identify) {
		String userId = identify.getUserId();
		Traits traits = identify.getTraits();
		
		Crashlytics.setUserIdentifier(userId);
		
		if (traits != null) {
			if (traits.has("email"))
				Crashlytics.setUserEmail(traits.getString("email"));
			
			if (traits.has("name"))
				Crashlytics.setUserName(traits.getString("name"));
		}
	}


}
