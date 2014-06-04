package com.segment.android.db;


import org.json.JSONException;
import org.json.JSONObject;

import com.segment.android.Logger;
import com.segment.android.models.Alias;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;

import android.util.Log;

public class JsonPayloadSerializer implements IPayloadSerializer {
	
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
			
			if (action.equals(Identify.ACTION)) {
				return new Identify(obj);
			} else if (action.equals(Track.ACTION)) {
				return new Track(obj);
			} else if (action.equals(Alias.ACTION)) {
				return new Alias(obj);
			} else if (action.equals(Group.ACTION)) {
				return new Group(obj);
			} else if (action.equals(Screen.ACTION)) {
				return new Screen(obj);
			} else {
				
			}
		} catch (JSONException e) {
			Logger.e("Failed to convert json to base payload: " + 
					Log.getStackTraceString(e));
		}
		
		return null;
	}

}
