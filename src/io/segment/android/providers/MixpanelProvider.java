package io.segment.android.providers;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Identify;
import io.segment.android.models.Track;
import io.segment.android.models.Traits;
import io.segment.android.provider.SimpleProvider;
import android.content.Context;
import android.text.TextUtils;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class MixpanelProvider extends SimpleProvider {

	private static class SettingKey { 

		private static final String TOKEN = "token";
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
		
		if (traits != null) {
			mixpanel.registerSuperProperties(traits);
		}
	}
	
	@Override
	public void track(Track track) {
		String userId = track.getUserId();
		String event = track.getEvent();
		EventProperties properties = track.getProperties();
		
		mixpanel.identify(userId);
		
		mixpanel.track(event, properties);
	}
	
	@Override
	public void flush() {
		if (mixpanel != null)
			mixpanel.flush();
	}
	
}
