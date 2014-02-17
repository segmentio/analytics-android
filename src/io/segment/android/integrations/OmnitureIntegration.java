package io.segment.android.integrations;

import io.segment.android.Logger;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.integration.SimpleIntegration;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;

import java.util.Hashtable;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.adobe.adms.measurement.ADMS_Measurement;

public class OmnitureIntegration extends SimpleIntegration {

	private static class SettingKey { 
		
		private static final String REPORT_SUITE_ID = "reportSuiteId";
		private static final String TRACKING_SERVER = "trackingServerUrl";
	}

	@Override
	public String getKey() {
		return "Omniture";
	}
	
	@Override
	public void validate(EasyJSONObject settings) throws InvalidSettingsException {
		
		if (TextUtils.isEmpty(settings.getString(SettingKey.REPORT_SUITE_ID)))
			throw new InvalidSettingsException(SettingKey.REPORT_SUITE_ID, "Omniture requires the reportSuiteId setting.");
		
		if (TextUtils.isEmpty(settings.getString(SettingKey.TRACKING_SERVER)))
			throw new InvalidSettingsException(SettingKey.TRACKING_SERVER, "Omniture requires the trackingServer setting.");
	}
	
	private void initialize(Context context) {

		EasyJSONObject settings = this.getSettings();
		
		String reportSuiteId = settings.getString(SettingKey.REPORT_SUITE_ID);
		String trackingServer = settings.getString(SettingKey.TRACKING_SERVER);

		ADMS_Measurement measurement = ADMS_Measurement.sharedInstance(context);
		measurement.configureMeasurement(reportSuiteId, trackingServer);
		
		ready();
	}
	
	@Override
	public void onCreate(Context context) {
		initialize(context);
	}
	
	@Override
	public void onActivityStart(Activity activity) {
		ADMS_Measurement measurement = ADMS_Measurement.sharedInstance(activity);
		measurement.startActivity(activity);
	}
	
	@Override
	public void onActivityStop(Activity activity) {
		ADMS_Measurement measurement = ADMS_Measurement.sharedInstance();
		measurement.stopActivity();
	}
	
	@Override
	public void screen(Screen screen) {
		// track a "Viewed SCREEN" event
		track(screen);
	}
	
	@Override
	public void track(Track track) {
		
		// context.providers.Omniture must be set to true
		// to avoid sending not mapped custom events (event1) to Omniture
		if (!track.getContext().isProviderStrictlyEnabled(getKey())) {
			Logger.d("Omniture track not triggered because context.providers.Omniture not set to true.");
			return;
		}
		
		String event = track.getEvent();
		EventProperties properties = track.getProperties();
		
		ADMS_Measurement measurement = ADMS_Measurement.sharedInstance();
		measurement.trackEvents(event, toObjectHashtable(properties));
	}
	
	public Hashtable<String, Object> toObjectHashtable(EasyJSONObject json) {
		Hashtable<String, Object> map = new Hashtable<String, Object>();
	
		@SuppressWarnings("unchecked")
		Iterator<String> it = json.keys();
		while (it.hasNext()) {
			String key = it.next();
			map.put(key, json.get(key));
		}
		
		return map;
	}

}
