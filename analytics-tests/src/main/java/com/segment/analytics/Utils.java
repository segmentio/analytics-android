package com.segment.analytics;

import java.util.LinkedHashMap;

public final class Utils {
  private Utils() {
    throw new AssertionError("No instances.");
  }

  /** Create a {@link com.segment.analytics.Traits} with only a randomly generated anonymous ID. */
  public static Traits createTraits() {
    return Traits.create();
  }

  /** Create a {@link com.segment.analytics.Traits} object with the given {@code userId}. */
  public static Traits createTraits(String userId) {
    return createTraits().putUserId(userId);
  }

  /** Create an {@link com.segment.analytics.AnalyticsContext} with the given {@code traits}. */
  public static AnalyticsContext createContext(Traits traits) {
    AnalyticsContext context = new AnalyticsContext(new LinkedHashMap<String, Object>());
    context.setTraits(traits);
    return context;
  }
}
