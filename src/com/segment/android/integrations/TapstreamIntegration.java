package com.segment.android.integrations;


import java.util.Iterator;

import org.OpenUDID.OpenUDID_manager;

import android.content.Context;
import android.text.TextUtils;

import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;

public class TapstreamIntegration extends SimpleIntegration {

	private static class SettingKey {
		private static final String ACCOUNT_NAME = "accountName";
		private static final String SDK_SECRET = "sdkSecret";
	}

	@Override
	public void validate(EasyJSONObject settings) throws InvalidSettingsException {
		if (TextUtils.isEmpty(settings.getString(SettingKey.ACCOUNT_NAME))) {
			throw new InvalidSettingsException(SettingKey.ACCOUNT_NAME,	"accountName required.");
		}
		if (TextUtils.isEmpty(settings.getString(SettingKey.SDK_SECRET))) {
			throw new InvalidSettingsException(SettingKey.SDK_SECRET, "sdkSecret required.");
		}
	}

	@Override
	public String getKey() {
		return "Tapstream";
	}

	private void initialize(Context context) {
		EasyJSONObject settings = this.getSettings();
		String accountName = settings.getString(SettingKey.ACCOUNT_NAME);
		String sdkSecret = settings.getString(SettingKey.SDK_SECRET);

		Config config = new Config();
		config.setOpenUdid(OpenUDID_manager.getOpenUDID());
		Tapstream.create(context, accountName, sdkSecret, config);
		
		ready();
	}

	@Override
	public void onCreate(Context context) {
		initialize(context);
	}

	@Override
	public void screen(Screen screen) {
		Tapstream.getInstance().fireEvent(makeEvent("screen-" + screen.getName(), screen.getProperties()));
	}

	@Override
	public void track(Track track) {
		Tapstream.getInstance().fireEvent(makeEvent(track.getEvent(), track.getProperties()));
	}

	private Event makeEvent(String name, Props properties) {
		Event e = new Event(name, false);
		if (properties != null) {
			Iterator<?> iter = properties.keys();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				e.addPair(key, properties.get(key));
			}
		}
		return e;
	}
}