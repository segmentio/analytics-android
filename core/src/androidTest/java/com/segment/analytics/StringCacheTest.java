package com.segment.analytics;

import com.segment.analytics.BaseAndroidTestCase;
import com.segment.analytics.StringCache;
import com.segment.analytics.Utils;

import static org.fest.assertions.api.Assertions.assertThat;

public class StringCacheTest extends BaseAndroidTestCase {
  private StringCache stringCache;

  @Override protected void setUp() throws Exception {
    super.setUp();
    stringCache = new StringCache(Utils.getSharedPreferences(getContext()), "cache-test");
    stringCache.delete();
    assertThat(stringCache.get()).isNullOrEmpty();
  }

  public void testSave() throws Exception {
    stringCache.set("hello");
    assertThat(stringCache.get()).isEqualTo("hello");
  }

  public void testCacheWithKeyHasSameValue() throws Exception {
    StringCache stringCacheDuplicate =
        new StringCache(Utils.getSharedPreferences(getContext()), "cache-test");
    assertThat(stringCacheDuplicate.get()).isNullOrEmpty();

    stringCache.set("hello");
    assertThat(stringCacheDuplicate.get()).isEqualTo("hello").isEqualTo(stringCache.get());

    stringCache.delete();
    assertThat(stringCacheDuplicate.get()).isNullOrEmpty();
  }
}
