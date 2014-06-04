package com.segment.android.integrations;

import android.content.Context;
import android.text.TextUtils;

import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;

public class CrittercismIntegration extends SimpleIntegration {

	private static class SettingKey { 

		private static final String APP_ID = "appId";
		
		private static final String DELAY_SENDING_APP_LOAD = "delaySendingAppLoad";
		private static final String INCLUDE_VERSION_CODE = "includeVersionCode";
		private static final String SHOULD_INCLUDE_LOGCAT = "shouldCollectLogcat";
	}
	
	@Override
	public String getKey() {
		return "Crittercism";
	}

	@Override
	public void validate(EasyJSONObject settings)
			throws InvalidSettingsException {

		if (TextUtils.isEmpty(settings.getString(SettingKey.APP_ID))) {
			throw new InvalidSettingsException(SettingKey.APP_ID, "Crittercism requires the appId setting.");
		}
	}

	@Override
	public void onCreate(Context context) {
		
		EasyJSONObject settings = this.getSettings();
		String appId = settings.getString(SettingKey.APP_ID);
		
		// docs: https://app.crittercism.com/developers/docs-optional-android
		CrittercismConfig config = new CrittercismConfig();
		// send app load data with Crittercism.sendAppLoadData()
		config.setDelaySendingAppLoad(settings.getBoolean(SettingKey.DELAY_SENDING_APP_LOAD, false));
		// necessary for collecting logcat data on Android Jelly Bean devices.
		config.setLogcatReportingEnabled(settings.getBoolean(SettingKey.SHOULD_INCLUDE_LOGCAT, false)); 
	    // include version code in version name.
	    config.setVersionCodeToBeIncludedInVersionString(settings.getBoolean(SettingKey.INCLUDE_VERSION_CODE, false));

		Crittercism.initialize(context, appId, config);
		
		ready();
	}
	
	@Override
	public void identify(Identify identify) {
		String userId = identify.getUserId();
		Traits traits = identify.getTraits();
		
		Crittercism.setUsername(userId);
		if (traits != null) {
			
			if (traits.has("name"))
				Crittercism.setUsername(traits.getString("name"));
			
			Crittercism.setMetadata(traits);
		}
	}
	
	@Override
	public void screen(Screen screen) {
		// track a "Viewed SCREEN" event
		track(screen);
	}
	
	@Override
	public void track(Track track) {
		String event = track.getEvent();
		Crittercism.leaveBreadcrumb(event);
	}
	
	@Override
	public void flush() {
		Crittercism.sendAppLoadData();
	}
}
