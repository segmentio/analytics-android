package com.segment.android.integrations;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.amplitude.api.Amplitude;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;

public class AmplitudeIntegration extends SimpleIntegration {

	private static class SettingKey { 

		private static final String API_KEY = "apiKey";
	}
	
	@Override
	public String getKey() {
		return "Amplitude";
	}
	
	@Override
	public void validate(EasyJSONObject settings)
			throws InvalidSettingsException {

		if (TextUtils.isEmpty(settings.getString(SettingKey.API_KEY))) {
			throw new InvalidSettingsException(SettingKey.API_KEY, "Amplitude requires the apiKey setting.");
		}
	}
	
	@Override
	public void onCreate(Context context) {
		
		EasyJSONObject settings = this.getSettings();
		String apiKey = settings.getString(SettingKey.API_KEY);
		
		Amplitude.initialize(context, apiKey);
		
		ready();
	}
	
	@Override
	public void onActivityStart(Activity activity) {
		Amplitude.startSession();
	}
	
	@Override
	public void onActivityStop(Activity activity) {
		Amplitude.endSession();
	}
	
	@Override
	public void identify(Identify identify) {
		String userId = identify.getUserId();
		Traits traits = identify.getTraits();
		
		Amplitude.setUserId(userId);
		Amplitude.setUserProperties(traits);
	}

	@Override
	public void screen(Screen screen) {
		event("Viewed " + screen.getName() + " Screen", screen.getProperties());
	}
	
	@Override
	public void track(Track track) {
		event(track.getEvent(), track.getProperties());
	}
	
	private void event(String name, Props properties) {
		if (properties != null && properties.has("revenue")) {
			double revenue = properties.getDouble("revenue", 0.0);
			Amplitude.logRevenue(revenue);
		}
		
		Amplitude.logEvent(name, properties);
	}
	
	@Override
	public void flush() {
		Amplitude.uploadEvents();
	}

}
