package com.segment.android.utils;

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

  public void scheduleNow() {
    scheduleTick(0);
  }

  private void scheduleTick() {
    scheduleTick(frequencyMs);
  }

  private void scheduleTick(int frequencyMs) {

    if (!active) return;

    Handler handler = handler();

    handler.postDelayed(tick, frequencyMs);
  }

  private Runnable tick = new Runnable() {

    @Override
    public void run() {

      if (active) {
        clock.run();

        scheduleTick();
      }
    }
  };

  @Override
  public void quit() {

    active = false;
    super.quit();
  }
}
