package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.CrashlyticsProvider;

public class CrashlyticsProviderTest  extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new CrashlyticsProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("apiKey", "fa998e30d6e54ddab65b764c84cb5183bbb5d748");
		return settings;
	}

}