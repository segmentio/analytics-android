package io.segment.android.flush;

import io.segment.android.Analytics;
import io.segment.android.Logger;
import io.segment.android.db.IPayloadDatabaseLayer;
import io.segment.android.db.IPayloadDatabaseLayer.PayloadCallback;
import io.segment.android.models.BasePayload;
import io.segment.android.models.Batch;
import io.segment.android.request.IRequester;
import io.segment.android.stats.AnalyticsStatistics;
import io.segment.android.utils.LooperThreadWithHandler;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

import android.os.Handler;
import android.util.Log;


/**
 * A Looper/Handler backed flushing thread
 *
 */
public class FlushThread extends LooperThreadWithHandler implements IFlushLayer {
		
	/**
	 * A factory to create a batch around a list of payload actions
	 */
	public interface BatchFactory {
		public Batch create(List<BasePayload> payloads);
	}
	
	private IRequester requester;
	private IPayloadDatabaseLayer databaseLayer;
	private BatchFactory batchFactory;

	public FlushThread(IRequester requester, 
					   BatchFactory batchFactory, 
					   IPayloadDatabaseLayer databaseLayer) {
		
		this.requester = requester;
		this.batchFactory = batchFactory;
		this.databaseLayer = databaseLayer;
	}
	
	/**
	 * Gets messages from the database, makes a request to the server,
	 * deletes the sent messages from the database, and continues flushing
	 * if there's messages left in the queue.
	 * @param callback
	 */
	public void flush(final FlushCallback callback) {
		
		// ask the database for the next payload batch
		databaseLayer.removeNextPayload(new PayloadCallback() {

			@Override
			public void onPayload(
					final long minId, 
					final long maxId,
					final List<BasePayload> payloads) {
				
				// we are currently executing on the database
				// thread so we're still technically locking the 
				// database
				
				if (payloads.size() > 0) {
					// we have things to flush
					
					final long start = System.currentTimeMillis();
					
					// let's create the batch frame that we're gonna flush
					Batch batch = batchFactory.create(payloads);
					
					// now let's make a request on the flushing thread
					performRequest(batch, new RequestCallback() {
						
						@Override
						public void onRequestCompleted(boolean success, Batch batch) {
							// we are now executing in the context of the
							// flushing thread
							
							if (success) Logger.i("Batch request succeeded");
							else Logger.e("Batch request failed.");
							
							AnalyticsStatistics statistics = Analytics.getStatistics();
							
							for (BasePayload payload : payloads) {
								if (success) {
									Logger.i("Item " + payload.toDescription() + " successfully sent.");
									statistics.updateSuccessful(1);
								} else {
									Logger.w("Item " + payload.toDescription() + " failed to be sent.");
									databaseLayer.enqueue(payload, null);
									Logger.i("Re-queued payload " + payload.getRequestId());
									statistics.updateFailed(1);
								}
							}
							
							if (!success) {
								// if we failed at flushing (connectivity issues), let's re-add them
								if (callback != null) callback.onFlushCompleted(false);
								
							} else {
								// we sent it successfully, let's update the request time 
								long duration = System.currentTimeMillis() - start;
								statistics.updateRequestTime(duration);
								
								// now we can initiate another flush to make
								// sure that there's nothing left 
								// in the database before we say we're fully flushed
								flush(callback);
							}
						}
					});
					
				} else {
					// there is nothing to flush, we're done
					if (callback != null) callback.onFlushCompleted(true);
				}
			}
		});
	}
	
	
	/**
	 * Callback for when a request to the server completes
	 *
	 */
	public interface RequestCallback {
		public void onRequestCompleted(boolean success, Batch batch);
	}
	
	/**
	 * Performs the request on the 
	 * @param batch
	 * @param callback
	 */
	private void performRequest(final Batch batch, 
								final RequestCallback callback) {
		
		Handler handler = handler();
		
		handler.post(new Runnable() {

			@Override
			public void run() {
				
				HttpResponse response = requester.send(batch);
				
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
				
				if (callback != null) callback.onRequestCompleted(success, batch);
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