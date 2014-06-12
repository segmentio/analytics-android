package com.segment.android;

import android.app.Activity;
import android.os.Bundle;

/**
 * 
 * A base activity that automatically configures your Segmnet.io analytics
 * provider. An alternative is overriding these methods: 
 * 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Analytics.onCreate(this);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.activityStart(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Analytics.activityResume(this);
	}
	
	@Override
	protected void onPause() {
		Analytics.activityPause(this);
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.activityStop(this);
	}
 * 
 * This activity automatically initializes the Segment.io Android client. 
 * 
 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
 * you to conveniently consume the API without making any HTTP requests
 * yourself.
 * 
 * This client is also designed to be thread-safe and to not block each of
 * your calls to make a HTTP request. It uses batching to efficiently send
 * your requests on a separate resource-constrained thread pool.
 * 
 */
public class TrackedActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Analytics.onCreate(this);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.activityStart(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Analytics.activityResume(this);
	}
	
	@Override
	protected void onPause() {
		Analytics.activityPause(this);
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.activityStop(this);
	}
	
}
