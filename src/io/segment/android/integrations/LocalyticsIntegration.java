package io.segment.android.integrations;

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

import com.localytics.android.LocalyticsSession;

public class LocalyticsIntegration extends SimpleIntegration {

	private static class SettingKey { 
		
		private static final String APP_KEY = "appKey";
	}
	
	private LocalyticsSession localyticsSession;
	
	@Override
	public String getKey() {
		return "Localytics";
	}

	@Override
	public void validate(EasyJSONObject settings) throws InvalidSettingsException {
		
		if (TextUtils.isEmpty(settings.getString(SettingKey.APP_KEY))) {
			throw new InvalidSettingsException(SettingKey.APP_KEY, "Localytics requires the appKey setting.");
		}
	}

	@Override
	public void onCreate(Context context) {
		// docs: http://www.localytics.com/docs/android-integration/
		EasyJSONObject settings = this.getSettings();
		String appKey = settings.getString(SettingKey.APP_KEY);
		
	    this.localyticsSession = new LocalyticsSession(context, appKey);
	    
	    this.localyticsSession.open();
	    this.localyticsSession.upload();
	    
	    ready();
	}

	@Override
	public void onActivityResume(Activity activity) {
		if (this.localyticsSession != null)
			this.localyticsSession.open();
	}

	@Override
	public void onActivityPause(Activity activity) {
		if (this.localyticsSession != null) {
			this.localyticsSession.close();
			this.localyticsSession.upload();
		}
	}
	
	@Override
	public void identify(Identify identify) {
		String userId = identify.getUserId();
		Traits traits = identify.getTraits();
		
		this.localyticsSession.setCustomerId(userId);
		
		if (traits != null) {
			if (traits.has("email"))
				this.localyticsSession.setCustomerEmail(traits.getString("email"));
			
			if (traits.has("name"))
				this.localyticsSession.setCustomerEmail(traits.getString("name"));
		
			@SuppressWarnings("unchecked")
			Iterator<String> it = traits.keys();
			while (it.hasNext()) {
				String key = it.next();
				String value = "" + traits.get(key);
				this.localyticsSession.setCustomerData(key, value);
			}
		}
	}
	
	@Override
	public void screen(Screen screen) {
		String screenName = screen.getScreen();
		this.localyticsSession.tagScreen(screenName);
	}
	
	@Override
	public void track(Track track) {
		String event = track.getEvent();
		Props properties = track.getProperties();
		
		Map<String, String> map = new HashMap<String, String>();
		if (properties != null) map = properties.toStringMap();
		
		this.localyticsSession.tagEvent(event, map);
	}
	
	@Override
	public void flush() {
		if (this.localyticsSession != null)
			this.localyticsSession.upload();
	}
}
