package com.segment.analytics;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class StringCacheTest {
  private StringCache stringCache;

  @Before public void setUp() {
    stringCache =
        new StringCache(Utils.getSharedPreferences(Robolectric.application), "string-cache-test");
    stringCache.delete();
    assertThat(stringCache.get()).isNullOrEmpty();
  }

  @Test public void save() throws Exception {
    stringCache.set("hello");
    assertThat(stringCache.get()).isEqualTo("hello");
  }

  @Test public void cacheWithKeyAndContextHasSameValue() throws Exception {
    StringCache stringCacheDuplicate =
        new StringCache(Utils.getSharedPreferences(Robolectric.application), "string-cache-test");
    assertThat(stringCacheDuplicate.get()).isNullOrEmpty();

    stringCache.set("hello");
    assertThat(stringCacheDuplicate.get()).isEqualTo("hello").isEqualTo(stringCache.get());

    stringCache.delete();
    assertThat(stringCacheDuplicate.get()).isNullOrEmpty();
  }
}
