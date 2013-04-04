package io.segment.android;

import java.util.concurrent.TimeUnit;

public class Defaults {

	public static final boolean DEBUG = false;
	
	public static final String HOST = "https://api.segment.io";

	public static final int FLUSH_AT = 20;
	public static final int FLUSH_AFTER = (int) TimeUnit.SECONDS.toMillis(10);
	public static final int MAX_QUEUE_SIZE = 10000;


}