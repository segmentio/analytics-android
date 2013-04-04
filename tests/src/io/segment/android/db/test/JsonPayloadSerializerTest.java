package io.segment.android.db.test;

import io.segment.android.db.JsonPayloadSerializer;
import io.segment.android.models.Context;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.Identify;
import io.segment.android.models.Traits;

import java.util.Date;

import org.junit.Test;

import android.test.AndroidTestCase;

public class JsonPayloadSerializerTest extends AndroidTestCase {

	
	
	@Test
	public void testIdentify() {

		JsonPayloadSerializer serializer = new JsonPayloadSerializer();
		
		Identify identify = new Identify("ilya@segment.io", 
				new Traits("name", "Achilles",
						   "email", "achilles@segment.io",
						   "subscriptionPlan", "Premium",
						   "friendCount", 29,
						   "company", new EasyJSONObject()
								.put("name", "Company, inc.")),
							new Date(),
							new Context().setIp("192.168.1.1"));
		
		String json = serializer.serialize(identify);
		
		System.out.println(json);

	}
	
}
