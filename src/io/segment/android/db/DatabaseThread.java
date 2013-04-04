package io.segment.android.db;

import io.segment.android.utils.SingletonHandlerThread;

public class DatabaseThread extends SingletonHandlerThread {
	
	private static DatabaseThread instance = new DatabaseThread();
	static {
		instance.start();
	}
	
	public static DatabaseThread getInstance() {
		return instance;
	}
	
}
