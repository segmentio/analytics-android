package io.segment.android.info;

import io.segment.android.models.EasyJSONObject;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.location.LocationManager;

public class Location implements Info<JSONObject> {

	@Override
	public String getKey() {
		return "location";
	}

	@Override
	public JSONObject get(Context context) {

		LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		List<String> providers = locationManager.getProviders(true);

		List<android.location.Location> locations = 
				new ArrayList<android.location.Location>();

		for (String provider : providers) {
			android.location.Location location = locationManager
					.getLastKnownLocation(provider);
			if (location != null) {
				locations.add(location);
			}
		}

		long maximumTimestamp = -1;
		android.location.Location bestLocation = null;

		for (android.location.Location location : locations) {
			if (location.getTime() > maximumTimestamp) {
				maximumTimestamp = location.getTime();
				bestLocation = location;
			}
		}

		EasyJSONObject object = new EasyJSONObject();
		
		if (bestLocation != null) {
			object.put("lat", bestLocation.getLatitude());
			object.put("lon", bestLocation.getLongitude());
			
			// you could figure out who your fastest user is. who doesnt want that?
			object.put("speed", bestLocation.getSpeed());
		}

		return object;
	}

}
