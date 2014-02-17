package io.segment.android.integrations;

import io.segment.android.Constants;
import io.segment.android.Logger;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.integration.SimpleIntegration;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.quantcast.measurement.service.QuantcastClient;

public class QuantcastIntegration extends SimpleIntegration {

	private static class SettingKey { 

		private static final String API_KEY = "apiKey";
	}
	
	private String apiKey;
	
	@Override
	public String getKey() {
		return "Quantcast";
	}
	
	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			Constants.Permission.ACCESS_NETWORK_STATE
		};
	}
	
	@Override
	public void validate(EasyJSONObject settings)
			throws InvalidSettingsException {

		if (TextUtils.isEmpty(settings.getString(SettingKey.API_KEY))) {
			throw new InvalidSettingsException(SettingKey.API_KEY, "Quantcast requires the apiKey setting.");
		}
	}
	
	@Override
	public void onCreate(Context context) {
		
		checkPermission(context);
		
		EasyJSONObject settings = this.getSettings();
		apiKey = settings.getString(SettingKey.API_KEY);
		
		QuantcastClient.enableLogging(Logger.isLogging());
		
		ready();
	}
	
	
	@Override
	public void onActivityStart(Activity activity) {
		if (!checkPermission(activity)) return;
		QuantcastClient.activityStart(activity, apiKey, null, null);
	}
	
	@Override
	public void onActivityStop(Activity activity) {
		if (!checkPermission(activity)) return;
		QuantcastClient.activityStop();
	}
	
	@Override
	public void identify(Identify identify) {
		if (!hasPermission) return;
		String userId = identify.getUserId();
		QuantcastClient.recordUserIdentifier(userId);
	}

	@Override
	public void screen(Screen screen) {
		if (!hasPermission) return;
		// track a "Viewed SCREEN" event
		track(screen);
	}
	
	@Override
	public void track(Track track) {
		if (!hasPermission) return; 
		String event = track.getEvent();
		QuantcastClient.logEvent(event);
	}

}
