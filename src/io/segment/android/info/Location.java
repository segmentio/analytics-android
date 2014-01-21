package io.segment.android.info;

import io.segment.android.models.EasyJSONObject;

import org.json.JSONObject;

import android.content.Context;
import android.location.Criteria;
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

		// http://stackoverflow.com/questions/5505429/switching-between-network-and-gps-provider
		// only read from the network to avoid turning on GPS
		Criteria crit = new Criteria();
		crit.setPowerRequirement(Criteria.POWER_LOW);
		crit.setAccuracy(Criteria.ACCURACY_COARSE);
		String provider = locationManager.getBestProvider(crit, true);
		
		EasyJSONObject object = new EasyJSONObject();

        if (provider != null) {
            android.location.Location location;

            try {
                location = locationManager.getLastKnownLocation(provider);
            } catch (SecurityException ex) {
                //The application may not have permission to access location
                location = null;
            }

			if (location != null) {
				object.put("latitude", location.getLatitude());
				object.put("longitude", location.getLongitude());
				
				// you could figure out who your fastest user is. who doesnt want that?
				object.put("speed", location.getSpeed());
			}
		}

		return object;
	}

}
