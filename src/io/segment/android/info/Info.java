package io.segment.android.info;

import org.json.JSONObject;

/**
 * A module that knows how to get a peice of information about
 * the phone
 *
 */
public interface Info<T> {

	/**
	 * Fetches the context key for this piece of info
	 * @return
	 */
	public String getKey();
	
	/**
	 * Returns a primitive object or a {@link JSONObject} that
	 * represents this piece of information
	 * @param context The Android Application Context
	 * @return
	 */
	public T get(android.content.Context context);
	
	
}
