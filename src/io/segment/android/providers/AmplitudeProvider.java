package io.segment.android.providers;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.models.Traits;
import io.segment.android.provider.SimpleProvider;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.amplitude.api.Amplitude;

public class AmplitudeProvider extends SimpleProvider {

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
		Amplitude.setGlobalUserProperties(traits);
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
		
		if (properties != null && properties.has("revenue")) {
			double revenue = properties.getDouble("revenue", 0.0);
			Amplitude.logRevenue(revenue);
		}
		
		Amplitude.logEvent(event, properties);
	}
	
	@Override
	public void flush() {
		Amplitude.uploadEvents();
	}

}
