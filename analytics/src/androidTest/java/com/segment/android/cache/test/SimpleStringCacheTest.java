/*
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

package com.segment.android.cache.test;

import android.content.Context;
import android.test.AndroidTestCase;
import com.segment.android.cache.SimpleStringCache;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleStringCacheTest extends AndroidTestCase {

  private SimpleStringCache noLoaderCache;
  private String val;

  @BeforeClass
  public void setup() {
    Context context = this.getContext();
    noLoaderCache = new SimpleStringCache(context, "cache.key");
    val = "cache value";
  }

  @Test
  public void getNull() {
    Assert.assertNull(noLoaderCache.get());
  }

  @Test
  public void set() {
    noLoaderCache.set(val);
    Assert.assertEquals(val, noLoaderCache.get());
  }
}
