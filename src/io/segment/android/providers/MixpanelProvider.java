package io.segment.android.providers;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.Alias;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Identify;
import io.segment.android.models.Track;
import io.segment.android.models.Traits;
import io.segment.android.provider.SimpleProvider;

import java.util.Iterator;

import android.content.Context;
import android.text.TextUtils;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class MixpanelProvider extends SimpleProvider {

	private static class SettingKey { 

		private static final String TOKEN = "token";
		
		private static final String PEOPLE = "people";
	}
	
	private MixpanelAPI mixpanel;
	
	@Override
	public String getKey() {
		return "Mixpanel";
	}
	
	@Override
	public void validate(EasyJSONObject settings)
			throws InvalidSettingsException {

		if (TextUtils.isEmpty(settings.getString(SettingKey.TOKEN))) {
			throw new InvalidSettingsException(SettingKey.TOKEN, "Mixpanel requires the token setting.");
		}
	}
	
	private boolean isMixpanelPeopleEnabled() {
		EasyJSONObject settings = this.getSettings();
		return settings.getBoolean(SettingKey.PEOPLE, false);
	}
	
	@Override
	public void onCreate(Context context) {

		EasyJSONObject settings = this.getSettings();
		String token = settings.getString(SettingKey.TOKEN);
		
		mixpanel = MixpanelAPI.getInstance(context, token);
		
		ready();
	}
	
	@Override
	public void identify(Identify identify) {
		String userId = identify.getUserId();
		Traits traits = identify.getTraits();
		
		mixpanel.identify(userId);
		
		if (traits != null)
			mixpanel.registerSuperProperties(traits);
		
		if (isMixpanelPeopleEnabled()) {
			MixpanelAPI.People people = mixpanel.getPeople();
			people.identify(userId);
			if (traits != null) {
				@SuppressWarnings("unchecked")
				Iterator<String> it = traits.keys();
				while (it.hasNext()) {
					String key = it.next();
					people.set(key, traits.get(key));
				}
			}
		}
	}
	
	@Override
	public void track(Track track) {
		String event = track.getEvent();
		EventProperties properties = track.getProperties();
				
		mixpanel.track(event, properties);
		
		if (isMixpanelPeopleEnabled()) {			
			// consider the charge
			if (properties != null && properties.has("revenue")) {
				MixpanelAPI.People people = mixpanel.getPeople();
				double revenue = properties.getDouble("revenue", 0.0);
				people.trackCharge(revenue, properties);
			}
		}
	}
	
	@Override
	public void alias(Alias alias) {
		String to = alias.getTo();
		
		mixpanel.identify(to);
	}
	
	@Override
	public void reset() {
		
		if (mixpanel != null)
			mixpanel.clearSuperProperties();
	}
	
	@Override
	public void flush() {
		if (mixpanel != null)
			mixpanel.flush();
	}
	
}
