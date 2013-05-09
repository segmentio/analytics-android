package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.CountlyProvider;

public class CountlyProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new CountlyProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("appKey", "caf520b7eec55e3f5da0a8739bc9208397335832");
		settings.put("serverUrl", "https://cloud.count.ly");
		return settings;
	}

}
