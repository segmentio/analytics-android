package com.segment.android.utils;


import java.util.Map;

import org.json.JSONObject;

import com.segment.android.models.EasyJSONObject;

public class Parameters {

	/**
	 * Returns a copy of passed in json object, with keys mapped 
	 * from => to as dictated by the second move parameter. 
	 * @param json The input json
	 * @param map Maps parameters from => to
	 * @return A copied object with the parameters mapped
	 */
	public static EasyJSONObject move(JSONObject json, Map<String, String> map) {
		
		EasyJSONObject copy = new EasyJSONObject(json);
		
		// go through all the keys we want to map from
		for (String fromKey : map.keySet()) {
			// if the json object has the from key
			if (copy.has(fromKey)) {
				// get the key that we want to set it to
				String toKey = map.get(fromKey);
					
				Object val = copy.get(fromKey);
				copy.remove(fromKey);
				copy.put(toKey, val);;
			}
		}
		
		return copy;
	}

}
