package io.segment.android.provider;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import android.content.Context;
import android.text.TextUtils;

public class ProviderTest extends BaseProviderTest {

	
	@Override
	public Provider getProvider() {
		return new SimpleProvider() {

			@Override
			public String getKey() {
				return "provider";
			}
			
			@Override
			public void validate(EasyJSONObject settings)
					throws InvalidSettingsException {

				if (TextUtils.isEmpty(settings.getString("apiKey"))) {
					throw new InvalidSettingsException("apiKey", "Test Provider requires the apiKey setting.");
				}
			}

			@Override
			public void onCreate(Context context) {
				ready();
			}
		};
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("apiKey", "testkey");
		return settings;
	}
	
	

}
