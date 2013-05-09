package io.segment.android.test;

import io.segment.android.Analytics;

import org.junit.AfterClass;
import org.junit.BeforeClass;

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
		
		Analytics.initialize(context, "testsecret");
	}
	
	@AfterClass
	protected void close() {
		Analytics.close();
	}
	
}
