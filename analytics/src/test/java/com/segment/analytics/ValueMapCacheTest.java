/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ValueMapCacheTest {

  private ValueMap.Cache<Traits> traitsCache;
  private Cartographer cartographer;

  @Before
  public void setUp() {
    cartographer = Cartographer.INSTANCE;
    traitsCache =
        new ValueMap.Cache<>(
            RuntimeEnvironment.application, cartographer, "traits-cache-test", "tag", Traits.class);
    traitsCache.delete();
    assertThat(traitsCache.get()).isNullOrEmpty();
  }

  @Test
  public void save() throws Exception {
    Traits traits = new Traits().putValue("foo", "bar");
    traitsCache.set(traits);
    assertThat(traitsCache.get()).isEqualTo(traits);
  }

  @Test
  public void cacheWithSameKeyHasSameValue() throws Exception {
    assertThat(traitsCache.isSet()).isFalse();
    Traits traits = new Traits().putValue("foo", "bar");
    traitsCache.set(traits);

    ValueMap.Cache<Traits> traitsCacheDuplicate =
        new ValueMap.Cache<>(
            RuntimeEnvironment.application, cartographer, "traits-cache-test", "tag", Traits.class);
    assertThat(traitsCacheDuplicate.isSet()).isTrue();
  }
}
