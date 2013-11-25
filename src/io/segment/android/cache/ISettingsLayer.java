package io.segment.android.cache;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.utils.IThreadedLayer;

/**
 * Handles flushing to the server endpoint
 *
 */
public interface ISettingsLayer extends IThreadedLayer {

	/**
	 * Callback for the #get method
	 *
	 */
	public interface SettingsCallback {
		
		/**
		 * Called when the settings have loaded from the server
		 * @param success True for successful flush, false for not. 
		 * @param object 
		 */
		public void onSettingsLoaded(boolean success, EasyJSONObject object);
	}
	
	/**
	 * Called to flush the queue
	 * @param callback
	 */
	public void fetch(SettingsCallback callback);
	
	
}
