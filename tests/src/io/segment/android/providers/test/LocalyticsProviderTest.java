package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.LocalyticsProvider;

public class LocalyticsProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new LocalyticsProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("appKey", "3ccfac0c5c366f11105f26b-c8ab109c-b6e1-11e2-88e8-005cf8cbabd8");
		return settings;
	}
	
}
