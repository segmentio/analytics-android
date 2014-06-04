package com.segment.android.integrations.mixpanel.test;


import java.util.Calendar;
import java.util.Random;

import org.junit.Test;

import com.segment.android.Analytics;
import com.segment.android.Constants;
import com.segment.android.integration.BaseIntegrationInitializationActivity;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.MixpanelIntegration;
import com.segment.android.models.Context;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Props;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;

import android.util.Log;

public class MixpanelAliasTest extends BaseIntegrationInitializationActivity {

	@Override
	public Integration getIntegration() {
		return new MixpanelIntegration();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("token", "89f86c4aa2ce5b74cb47eb5ec95ad1f9");
		settings.put("people", true);
		return settings;
	}
	
	@Test
	public void testAlias() {
		
		reachReadyState();
		
		int random = (new Random()).nextInt();
		
		String sessionId = "android_session_id_" + random;
		String userId = "android_user_id_" + random;
		
		Props properties = new Props("revenue", 10.00);
		Traits traits = new Traits("$first_name", "Ilya " + random);
		Calendar timestamp = Calendar.getInstance();
		Context context = new Context();
		
		Analytics.setSessionId(sessionId);
		
		Log.e(Constants.TAG, "Mixpanel alias test is using session_id: " + 
				sessionId + ", and user_id: " + userId);
		
		integration.track(new Track(sessionId, null, "Anonymous Event", 
				properties, timestamp, context));
		
		integration.identify(new Identify(sessionId, userId, traits, timestamp, context));
		
		integration.track(new Track(sessionId, userId, "Identified Event", properties, timestamp, context));
		
		integration.flush();
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
