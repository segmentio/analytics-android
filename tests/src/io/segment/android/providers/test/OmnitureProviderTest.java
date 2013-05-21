package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.OmnitureProvider;

public class OmnitureProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new OmnitureProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("reportSuiteId", "xxxxxxxxx");
		settings.put("trackingServerUrl", "http://api.omniture.com");
		return settings;
	}

}