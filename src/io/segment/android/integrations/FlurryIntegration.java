package io.segment.android.integrations;

import io.segment.android.Logger;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.integration.SimpleIntegration;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Props;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.models.Traits;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;


public class FlurryIntegration extends SimpleIntegration {
	
	private static class SettingKey { 
		
		private static final String API_KEY = "apiKey";
		
		private static final String SESSION_LENGTH = "sessionLength";
		private static final String CAPTURE_UNCAUGHT_EXCEPTIONS = "captureUncaughtExceptions";
		private static final String USE_HTTPS = "useHttps";
	}
	
	@Override
	public void validate(EasyJSONObject settings) throws InvalidSettingsException {
		
		if (TextUtils.isEmpty(settings.getString(SettingKey.API_KEY))) {
			throw new InvalidSettingsException(SettingKey.API_KEY, "API Key (apiKey) required.");
		}
	}
	
	@Override
	public String getKey() {
		return "Flurry";
	}

	private void initialize() {

		EasyJSONObject settings = this.getSettings();
		
		int sessionLength = settings.getInt(SettingKey.SESSION_LENGTH, 10000);
		boolean captureUncaughtExceptions = settings.getBoolean(SettingKey.CAPTURE_UNCAUGHT_EXCEPTIONS, false);
		boolean useHttps = settings.getBoolean(SettingKey.USE_HTTPS, false);
		
		FlurryAgent.setContinueSessionMillis(sessionLength);
		FlurryAgent.setCaptureUncaughtExceptions(captureUncaughtExceptions);
		FlurryAgent.setUseHttps(useHttps);
	}

	@Override
	public void onCreate(Context context) {
		initialize();
		ready(); // should be ready so that onActivityStart(..) can run
	}
	
	@Override
	public void onActivityStart(Activity activity) {
		EasyJSONObject settings = this.getSettings();
		String apiKey = settings.getString(SettingKey.API_KEY);
		FlurryAgent.onStartSession(activity, apiKey);
	}
	
	@Override
	public void onActivityStop(Activity activity) {
		try {
			FlurryAgent.onEndSession(activity);
		} catch (NullPointerException e) {
			Logger.w("Flurry Agent's #onEndSession threw a NullPointerException.", e);
		}
	}
	
	@Override
	public void identify(Identify identify) {
		Traits traits = identify.getTraits();
		
		String gender = traits.getString("gender");
		if (!TextUtils.isEmpty(gender)) {
			if (gender.equalsIgnoreCase("male")) {
				FlurryAgent.setGender(Constants.MALE);
			} else if (gender.equalsIgnoreCase("female")) {
				FlurryAgent.setGender(Constants.FEMALE);
			}
		}
		
		Integer age = traits.getInt("age", null);
		if (age != null && age > 0) FlurryAgent.setAge(age);

		String userId = identify.getUserId();
		FlurryAgent.setUserId(userId);
	}
	

	@Override
	public void screen(Screen screen) {
		// increment flurry's page view count
		FlurryAgent.onPageView();
		// track a "Viewed SCREEN" event
		track(screen);
	}
	
	@Override
	public void track(Track track) {
		String event = track.getEvent();
		Props properties = track.getProperties();
		
		FlurryAgent.logEvent(event, toMap(properties));
	}

	private Map<String, String> toMap(Props properties) {
		Map<String, String> map = new HashMap<String, String>();
		
		if (properties != null) {
			@SuppressWarnings("unchecked")
			Iterator<String> it = properties.keys();
			while (it.hasNext()) {
				String key = it.next();
				String value = "" + properties.get(key);
				if (value.length() > 255) value = value.substring(0, 255);
				map.put(key, value);
			}
		}
		
		return map;
	}


}

