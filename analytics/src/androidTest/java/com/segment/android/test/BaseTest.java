package com.segment.android.test;


import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.segment.android.Analytics;
import com.segment.android.Config;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

public class BaseTest extends AndroidTestCase {

	protected Context context;
	
	@BeforeClass
	protected void setUp() {

		//delegates to the given context, but performs database and 
		// file operations with a renamed database/file 
		// name (prefixes default names with a given prefix).
		context = new RenamingDelegatingContext(getContext(), "test_");
		
		if (Analytics.isInitialized()) Analytics.close();
		
		// the https://segment.io/segmentio/android-test project
		Analytics.initialize(context, "5m6gbdgho6", new Config().setDebug(true));
	}
	
	@AfterClass
	protected void close() {
		Analytics.close();
	}
	
}
