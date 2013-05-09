package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.AmplitudeProvider;

public class AmplitudeProviderTest extends BaseProviderTest {
	
	@Override
	public Provider getProvider() {
		return new AmplitudeProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("apiKey", "07808866adb2510adf19ee69e8fc2201");
		return settings;
	}

}
