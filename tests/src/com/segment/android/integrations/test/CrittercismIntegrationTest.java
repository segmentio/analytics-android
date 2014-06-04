package com.segment.android.integrations.test;


import org.junit.Test;

import com.crittercism.app.Crittercism;
import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.CrittercismIntegration;
import com.segment.android.models.EasyJSONObject;

public class CrittercismIntegrationTest extends BaseIntegrationTest {

	@Override
	public Integration getIntegration() {
		return new CrittercismIntegration();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("appId", "51832f1a8b2e333349000007");
		settings.put("delaySendingAppLoad", false);
		settings.put("includeVersionCode", true);
		settings.put("shouldCollectLogcat", true);
		return settings;
	}

	@Test
	public void testHandledException() {
		reachReadyState();
		
		Crittercism.logHandledException(new RuntimeException("handled exception!")); 
		
		integration.flush();
	}
	
}
