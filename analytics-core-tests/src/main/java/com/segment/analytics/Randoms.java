package com.segment.analytics;

import java.util.Random;

public final class Randoms {

  private static final Random RANDOM = new Random();

  private Randoms() {
    throw new AssertionError("No instances");
  }

  public static boolean nextBoolean() {
    return RANDOM.nextBoolean();
  }
}
