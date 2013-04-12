package io.segment.android.utils;

import android.os.Handler;

public class HandlerTimer extends LooperThreadWithHandler {
	
	private Runnable clock;
	private boolean active;
	private int frequencyMs;
	
	public HandlerTimer(int frequencyMs, Runnable clock) {
		this.frequencyMs = frequencyMs;
		this.clock = clock;
	}
	
	@Override
	public synchronized void start() {
		super.start();
		
		active = true;
		
		scheduleTick();
	}
	
	private void scheduleTick() {
		
		if (!active) return;
		
		Handler handler = handler();
		
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {

				if (active) {
					clock.run();
				
					scheduleTick();
				}
			}
			
		}, frequencyMs);
	}
	
	@Override
	public void quit() {
		
		active = false;
		super.quit();
		
	}
}
