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

import java.util.Map;

class WearScreenPayload extends ValueMap {

    private static final String CATEGORY_KEY = "category";
    private static final String NAME_KEY = "name";
    private static final String PROPERTIES_KEY = "properties";

    // For deserialization
    WearScreenPayload(Map<String, Object> delegate) {
        super(delegate);
    }

    WearScreenPayload(String category, String name, Properties properties) {
        put(CATEGORY_KEY, category);
        put(NAME_KEY, name);
        put(PROPERTIES_KEY, properties);
    }

    Properties getProperties() {
        return getValueMap(PROPERTIES_KEY, Properties.class);
    }

    String getCategory() {
        return getString(CATEGORY_KEY);
    }

    String getName() {
        return getString(NAME_KEY);
    }
}
