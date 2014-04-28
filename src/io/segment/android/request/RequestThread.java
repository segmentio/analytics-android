package io.segment.android.request;

import io.segment.android.Analytics;
import io.segment.android.Logger;
import io.segment.android.models.Batch;
import io.segment.android.utils.LooperThreadWithHandler;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

import android.os.Handler;
import android.util.Log;

/**
 * A Looper/Handler backed request thread
 *
 */
public class RequestThread extends LooperThreadWithHandler implements IRequestLayer {
	
	private IRequester requester;

	public RequestThread(IRequester requester) {
		this.requester = requester;
	}

	/**
	 * Performs the request to the server.
	 * @param batch the action batch to send 
	 * @param callback 
	 */
	public void send(final Batch batch, final RequestCallback callback) {
		
		Handler handler = handler();
		
		handler.post(new Runnable() {

			@Override
			public void run() {
				long start = System.currentTimeMillis();
				
				// send the actual request
				HttpResponse response = requester.send(batch);
				
				long duration = System.currentTimeMillis() - start;
				Analytics.getStatistics().updateRequestTime(duration);
				
				boolean success = false;
				
				if (response == null) {
					// there's been an error
					Logger.w("Failed to make request to the server.");
					
				} else if (response.getStatusLine().getStatusCode() != 200) {
					
					try {
						// there's been a server error
						String responseBody = EntityUtils.toString(response.getEntity());
						
						Logger.e("Received a failed response from the server." + responseBody);
						
					} catch (ParseException e) {
						Logger.w("Failed to parse the response from the server." + 
								Log.getStackTraceString(e));
					} catch (IOException e) {
						Logger.w("Failed to read the response from the server." + 
								Log.getStackTraceString(e));
					}
				
				} else {
					
					Logger.d("Successfully sent a batch to the server");
					
					success = true;
				}
				
				if (callback != null) callback.onRequestCompleted(success);
			}
			
		});
	}
	
	/**
	 * Allow custom {link {@link IRequester} for testing.
	 * @param requester
	 */
	public void setRequester(IRequester requester) {
		this.requester = requester;
	}
	
}