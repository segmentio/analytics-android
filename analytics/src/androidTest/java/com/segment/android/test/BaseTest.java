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

package com.segment.android.test;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import com.segment.android.Analytics;
import com.segment.android.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class BaseTest extends AndroidTestCase {

  protected Context context;

  @BeforeClass
  protected void setUp() {

    //delegates to the given context, but performs database and
    // file operations with a renamed database/file
    // name (prefixes default names with a given prefix).
    context = new RenamingDelegatingContext(getContext(), "test_");

    if (Analytics.isInitialized()) Analytics.close();

    // the https://segment.io/segmentio/android-test project
    Analytics.initialize(context, "5m6gbdgho6", new Config().setDebug(true));
  }

  @AfterClass
  protected void close() {
    Analytics.close();
  }
}
