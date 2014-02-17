package io.segment.android.integrations;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.integration.SimpleIntegration;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;

import java.util.HashMap;
import java.util.Map;

import ly.count.android.api.Countly;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

public class CountlyIntegration extends SimpleIntegration {

	private static class SettingKey { 

		private static final String SERVER_URL = "serverUrl";
		private static final String APP_KEY = "appKey";
	}

	@Override
	public String getKey() {
		return "Countly";
	}

	@Override
	public void validate(EasyJSONObject settings)
			throws InvalidSettingsException {

		if (TextUtils.isEmpty(settings.getString(SettingKey.SERVER_URL))) {
			throw new InvalidSettingsException(SettingKey.SERVER_URL, "Countly requires the serverUrl setting.");
		}
		
		if (TextUtils.isEmpty(settings.getString(SettingKey.APP_KEY))) {
			throw new InvalidSettingsException(SettingKey.APP_KEY, "Amplitude requires the appKey setting.");
		}
	}
	
	@Override
	public void onCreate(Context context) {
		
		EasyJSONObject settings = this.getSettings();
		String serverUrl = settings.getString(SettingKey.SERVER_URL);
		String appKey = settings.getString(SettingKey.APP_KEY);
		
		Countly.sharedInstance().init(context, serverUrl, appKey);
		
		ready();
	}
	
	@Override
	public void onActivityStart(Activity activity) {
		Countly.sharedInstance().onStart();
	}

	@Override
	public void onActivityStop(Activity activity) {
		Countly.sharedInstance().onStop();
	}

	@Override
	public void screen(Screen screen) {
		// track a "Viewed SCREEN" event
		track(screen);
	}
	
	@Override
	public void track(Track track) {
		String event = track.getEvent();
		EventProperties properties = track.getProperties();
		
		Map<String, String> segmentation = new HashMap<String, String>();
		int count = 0;
		
		if (properties != null) {
			
			segmentation = properties.toStringMap();
			
			if (properties.has("sum")) {
				count = properties.getInt("sum", 0);
			}
		}
		
		Countly.sharedInstance().recordEvent(event, segmentation, count);
	}
}
