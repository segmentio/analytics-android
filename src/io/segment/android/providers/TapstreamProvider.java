package io.segment.android.providers;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.provider.SimpleProvider;

import java.lang.reflect.Method;
import java.util.Iterator;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;

public class TapstreamProvider extends SimpleProvider {

	private static class SettingKey {
		private static final String ACCOUNT_NAME = "accountName";
		private static final String DEVELOPER_SECRET = "developerSecret";
	}

	@Override
	public void validate(EasyJSONObject settings) throws InvalidSettingsException {
		if (TextUtils.isEmpty(settings.getString(SettingKey.ACCOUNT_NAME))) {
			throw new InvalidSettingsException(SettingKey.ACCOUNT_NAME,	"accountName required.");
		}
		if (TextUtils.isEmpty(settings.getString(SettingKey.DEVELOPER_SECRET))) {
			throw new InvalidSettingsException(SettingKey.DEVELOPER_SECRET,	"developerSecret required.");
		}
	}

	@Override
	public String getKey() {
		return "Tapstream";
	}

	private Method lookupMethod(String propertyName, Class<?> argType) {
		String methodName = propertyName;
		if (methodName.length() > 0) {
			methodName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
		}
		methodName = "set" + methodName;

		Method method = null;
		try {
			method = Config.class.getMethod(methodName, argType);
		} catch (NoSuchMethodException e) {
			Log.i(getClass().getSimpleName(), "Ignoring config field named '" + propertyName + "', probably not meant for this platform.");
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Error getting Config setter method: " + e.getMessage());
		}
		return method;
	}

	private void initialize(Context context) {
		EasyJSONObject settings = this.getSettings();
		String accountName = settings.getString(SettingKey.ACCOUNT_NAME);
		String developerSecret = settings.getString(SettingKey.DEVELOPER_SECRET);

		Config config = new Config();
		Iterator<?> iter = settings.keys();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			Object value = settings.get(key);

			if (value == null) {
				Log.e("Tapstream", "Config object will not accept null values, skipping field named: " + key);
				continue;
			}

			try {
				if (value instanceof String) {
					Method method = lookupMethod(key, String.class);
					if (method != null) {
						method.invoke(config, (String) value);
					}
				} else if (value instanceof Boolean) {
					Method method = lookupMethod(key, boolean.class);
					if (method != null) {
						method.invoke(config, (Boolean) value);
					}
				} else if (value instanceof Integer) {
					Method method = lookupMethod(key, int.class);
					if (method != null) {
						method.invoke(config, (Integer) value);
					}
				} else if (value instanceof Float) {
					Method method = lookupMethod(key, float.class);
					if (method != null) {
						method.invoke(config, (Float) value);
					}
				} else {
					Log.e("Tapstream", "Config object will not accept type: " + value.getClass().toString());
				}
			} catch (Exception e) {
				Log.e("Tapstream", "Error setting field on config object (key=" + key + "). " + e.getMessage());
			}
		}

		Tapstream.create(context, accountName, developerSecret, config);
		ready();
	}

	@Override
	public void onCreate(Context context) {
		initialize(context);
	}

	@Override
	public void onActivityStart(Activity activity) {
	}

	@Override
	public void onActivityStop(Activity activity) {
	}

	@Override
	public void identify(Identify identify) {
		// Tapstream doesn't use a identify event
	}

	@Override
	public void screen(Screen screen) {
		String name = screen.getScreen();
		if (name == null) {
			name = "";
		}
		Tapstream.getInstance().fireEvent(makeEvent("screen-" + name, screen));
	}

	@Override
	public void track(Track track) {
		String name = track.getEvent();
		if (name == null) {
			name = "";
		}
		Tapstream.getInstance().fireEvent(makeEvent(name, track));
	}

	private Event makeEvent(String name, Track track) {
		io.segment.android.models.Context context = track.getContext();

		boolean oneTimeOnly = false;
		if (context != null) {
			try {
				oneTimeOnly = context.getBoolean("oneTimeOnly");
			} catch (JSONException e) {
			}
		}
		Event e = new Event(name, oneTimeOnly);

		EventProperties properties = track.getProperties();
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
