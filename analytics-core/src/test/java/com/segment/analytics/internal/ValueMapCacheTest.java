package com.segment.analytics.internal;

import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class ValueMapCacheTest {
  private ValueMap.Cache<Traits> traitsCache;

  @Before public void setUp() {
    traitsCache = new ValueMap.Cache<>(Robolectric.application, "traits-cache-test", Traits.class);
    traitsCache.delete();
    assertThat(traitsCache.get()).isNullOrEmpty();
  }

  @Test public void save() throws Exception {
    Traits traits = new Traits().putValue("foo", "bar");
    traitsCache.set(traits);
    assertThat(traitsCache.get()).isEqualTo(traits);
  }

  @Test public void cacheWithKeyAndContextHasSameValue() throws Exception {
    assertThat(traitsCache.isSet()).isFalse();
    Traits traits = new Traits().putValue("foo", "bar");
    traitsCache.set(traits);

    ValueMap.Cache<Traits> traitsCacheDuplicate =
        new ValueMap.Cache<Traits>(Robolectric.application, "traits-cache-test", Traits.class);
    assertThat(traitsCacheDuplicate.isSet()).isTrue();
  }
}
