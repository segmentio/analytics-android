/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android.info;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationManager;
import com.segment.android.models.EasyJSONObject;
import org.json.JSONObject;

import static com.segment.android.utils.Utils.getSystemService;

public class Location implements Info<JSONObject> {

  @Override
  public String getKey() {
    return "location";
  }

  @Override
  public JSONObject get(Context context) {

    LocationManager locationManager = getSystemService(context, Context.LOCATION_SERVICE);

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
