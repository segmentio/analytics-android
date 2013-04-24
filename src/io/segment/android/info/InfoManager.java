package io.segment.android.info;

import io.segment.android.models.EasyJSONObject;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

/**
 * A manager that uses plugin information getters to construct
 *  an object that contains contextual information about the Android device
 *
 */
public class InfoManager {

	private List<Info<?>> managers; 
	
	public InfoManager() {
		managers = new LinkedList<Info<?>>();

		managers.add(new Build());
		managers.add(new Device());
		managers.add(new Display());
		managers.add(new Locale());
		managers.add(new Location());
		managers.add(new Telephony());
		managers.add(new Wifi());
	}
	
	
	/**
	 * Builds an object that contains contextual information about the phone
	 * @param context Android context for the phone
	 * @return JSONObject containing parsed information about the phone
	 */
	public JSONObject build(android.content.Context context) {
		
		EasyJSONObject info = new EasyJSONObject();
		
		for (Info<?> manager : managers) {
			String key = manager.getKey();
			Object val = manager.get(context);
			
			if (val != null) {
				info.putObject(key, val);
			}
		}
		
		return info;
	}
	
}
