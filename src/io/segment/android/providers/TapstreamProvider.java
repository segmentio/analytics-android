package io.segment.android.providers;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.provider.SimpleProvider;

import java.util.Iterator;

import org.OpenUDID.OpenUDID_manager;

import android.content.Context;
import android.text.TextUtils;

import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;

public class TapstreamProvider extends SimpleProvider {

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
		String name = screen.getScreen();
		Tapstream.getInstance().fireEvent(makeEvent("screen-" + name, screen.getProperties()));
	}

	@Override
	public void track(Track track) {
		String name = track.getEvent();
		Tapstream.getInstance().fireEvent(makeEvent(name, track.getProperties()));
	}

	private Event makeEvent(String name, EventProperties properties) {
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