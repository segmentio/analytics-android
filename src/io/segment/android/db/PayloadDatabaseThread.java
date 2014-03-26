package io.segment.android.db;

import io.segment.android.Constants;
import io.segment.android.models.BasePayload;
import io.segment.android.utils.LooperThreadWithHandler;

import java.util.LinkedList;
import java.util.List;

import android.os.Handler;
import android.util.Pair;

/**
 * The DatabaseThread is a singleton handler thread that is
 * statically created to assure a single entry point into the
 * database to achieve SQLLite thread safety.
 *
 */
public class PayloadDatabaseThread extends LooperThreadWithHandler 
								   implements IPayloadDatabaseLayer {

	private PayloadDatabase database;
	
	public PayloadDatabaseThread(PayloadDatabase database) { 
		this.database = database;
	}
	
	public void enqueue(final BasePayload payload,
						final EnqueueCallback callback) {
		
		Handler handler = handler();
		handler.post(new Runnable() {

			@Override
			public void run() {
				boolean success = database.addPayload(payload);
				long rowCount = database.getRowCount();
				
				if (callback != null) callback.onEnqueue(success, rowCount);
			}
		});
	}
	
	public void nextPayload(final PayloadCallback callback) {
		
		Handler handler = handler();
		handler.post(new Runnable() {

			@Override
			public void run() {
				
				List<Pair<Long, BasePayload>> pairs = database.getEvents(Constants.MAX_FLUSH);
				
				long minId = 0;
				long maxId = 0;
				
				List<BasePayload> payloads = new LinkedList<BasePayload>();
				
				if (pairs.size() > 0) {
					minId = pairs.get(0).first;
					maxId = pairs.get(pairs.size() - 1).first;
					
					for (Pair<Long, BasePayload> pair : pairs) {
						payloads.add(pair.second);
					}
				}
				
				if (callback != null)
					callback.onPayload(minId, maxId, payloads);
			}
		});	
	}
	
	public void removePayloads(final long minId, 
							   final long maxId, 
							   final RemoveCallback callback) {
		
		Handler handler = handler();
		handler.post(new Runnable() {

			@Override
			public void run() {
				
				int removed = database.removeEvents(minId, maxId);
				
				if (callback != null)
					callback.onRemoved(removed);
				
			}
		});	
	}

	@Override
	public void removeNextPayload(final PayloadCallback callback) {
		nextPayload(new PayloadCallback() {
			
			@Override
			public void onPayload(final long minId, final long maxId, final List<BasePayload> payloads) {
				if (payloads.size() == 0) {
					callback.onPayload(minId, maxId, payloads);
				} else {
					removePayloads(minId, maxId, new RemoveCallback() {
						
						@Override
						public void onRemoved(int removed) {
							callback.onPayload(minId, maxId, payloads);
						}
					});
				}
			}
		});
	}
	
}
