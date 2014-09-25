package com.segment.analytics;

import android.content.Context;

import static com.segment.analytics.Utils.getSharedPreferences;
import static com.segment.analytics.Utils.isNullOrEmpty;

class TraitsCache {
  private static final String TRAITS_CACHE_PREFIX = "traits-";

  private final StringCache stringCache;
  private final Traits traits;

  TraitsCache(Context context, String tag) {
    stringCache = new StringCache(getSharedPreferences(context), TRAITS_CACHE_PREFIX + tag);
    if (isNullOrEmpty(stringCache.get())) {
      traits = new Traits(context);
    } else {
      traits = new Traits(stringCache.get());
    }
  }

  Traits get() {
    return traits;
  }

  void save() {
    stringCache.set(traits.toString());
  }
}
