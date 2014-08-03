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

package com.segment.android;

import java.util.Map;

class ScreenPayload extends Payload {
  /** The category of the page or screen. We recommend using title case, like Docs. */
  private static final String CATEGORY_KEY = "category";

  /** The name of the page or screen. We recommend using title case, like About. */
  private static final String NAME_KEY = "name";

  /** The page and screen methods also take a properties dictionary, just like track. */
  private static final String PROPERTIES_KEY = "properties";

  ScreenPayload(String anonymousId, AnalyticsContext context, Map<String, Boolean> integrations,
      String userId, String category, String name, Properties properties) {
    super(Type.screen, anonymousId, context, integrations, userId);
    put(CATEGORY_KEY, category);
    put(NAME_KEY, name);
    put(PROPERTIES_KEY, properties);
  }

  String getCategory() {
    return getString(CATEGORY_KEY);
  }

  String getName() {
    return getString(NAME_KEY);
  }

  Properties getProperties() {
    return (Properties) get(PROPERTIES_KEY);
  }
}
