package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.BugsnagProvider;

import org.junit.Test;

import com.bugsnag.android.Bugsnag;

public class BugsnagProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new BugsnagProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("apiKey", "7563fdfc1f418e956f5e5472148759f0");
		return settings;
	}

	@Test
	public void testException() {
		reachReadyState();
		
		Bugsnag.notify(new RuntimeException("Non-fatal"));
	}
	
}

