package com.segment.android.info;


import org.json.JSONObject;

import com.segment.android.models.EasyJSONObject;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class Display implements Info<JSONObject> {

	@Override
	public String getKey() {
		return "display";
	}

	@Override
	public JSONObject get(Context context) {

		EasyJSONObject object = new EasyJSONObject();

		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		
		if (manager != null) {
			
			DisplayMetrics metrics = new DisplayMetrics();
			
			android.view.Display display = manager.getDefaultDisplay();
			display.getMetrics(metrics);
			
			object.put("height", metrics.heightPixels);
			object.put("width", metrics.widthPixels);
			
			object.put("density", metrics.density);
		}
        
		return object;
	}
	
}
