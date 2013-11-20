package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.TapstreamProvider;

public class TapstreamProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new TapstreamProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("accountName", "segmentio");
		settings.put("sdkSecret", "FHH7ypHPQQ2EyNxGIidatQ");
		return settings;
	}

}