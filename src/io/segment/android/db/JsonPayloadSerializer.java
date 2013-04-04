package io.segment.android.db;

import io.segment.android.models.Alias;
import io.segment.android.models.BasePayload;
import io.segment.android.models.Identify;
import io.segment.android.models.Track;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsonPayloadSerializer implements IPayloadSerializer {

	private static final String TAG = JsonPayloadSerializer.class.getName();
	
	@Override
	public String serialize(BasePayload payload) {
		return payload.toString();
	}

	@Override
	public BasePayload deseralize(String str) {
		
		JSONObject obj;
		
		try {
			obj = new JSONObject(str);
			
			String action = obj.getString("action");
			
			if (action == "identify") {
				return (Identify) obj;
			} else if (action == "track") {
				return (Track) obj;
			} else if (action == "alias") {
				return (Alias) obj;
			} else {
				
			}
		} catch (JSONException e) {
			Log.e(TAG, "Failed to convert json to base payload: " + 
					Log.getStackTraceString(e));
		}
		
		return null;
	}

}
