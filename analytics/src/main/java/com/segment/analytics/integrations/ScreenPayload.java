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
package com.segment.analytics.integrations;

import static com.segment.analytics.internal.Utils.assertNotNull;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.segment.analytics.Properties;
import com.segment.analytics.internal.Private;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScreenPayload extends BasePayload {

  static final String CATEGORY_KEY = "category";
  static final String NAME_KEY = "name";
  static final String PROPERTIES_KEY = "properties";

  @Private
  ScreenPayload(
      @NonNull String messageId,
      @NonNull Date timestamp,
      @NonNull Map<String, Object> context,
      @NonNull Map<String, Object> integrations,
      @Nullable String userId,
      @NonNull String anonymousId,
      @Nullable String name,
      @Nullable String category,
      @NonNull Map<String, Object> properties) {
    super(Type.screen, messageId, timestamp, context, integrations, userId, anonymousId);
    if (!isNullOrEmpty(name)) {
      put(NAME_KEY, name);
    }
    if (!isNullOrEmpty(category)) {
      put(CATEGORY_KEY, category);
    }
    put(PROPERTIES_KEY, properties);
  }

  /** The category of the page or screen. We recommend using title case, like "Docs". */
  @Nullable
  @Deprecated
  public String category() {
    return getString(CATEGORY_KEY);
  }

  /** The name of the page or screen. We recommend using title case, like "About". */
  @Nullable
  public String name() {
    return getString(NAME_KEY);
  }

  /** Either the name or category of the screen payload. */
  @NonNull
  public String event() {
    String name = name();
    if (!isNullOrEmpty(name)) {
      return name;
    }
    return category();
  }

  /** The page and screen methods also take a properties dictionary, just like track. */
  @NonNull
  public Properties properties() {
    return getValueMap(PROPERTIES_KEY, Properties.class);
  }

  @Override
  public String toString() {
    return "ScreenPayload{name=\"" + name() + ",category=\"" + category() + "\"}";
  }

  @NonNull
  @Override
  public ScreenPayload.Builder toBuilder() {
    return new Builder(this);
  }

  /** Fluent API for creating {@link ScreenPayload} instances. */
  public static class Builder extends BasePayload.Builder<ScreenPayload, Builder> {

    private String name;
    private String category;
    private Map<String, Object> properties;

    public Builder() {
      // Empty constructor.
    }

    @Private
    Builder(ScreenPayload screen) {
      super(screen);
      name = screen.name();
      properties = screen.properties();
    }

    @NonNull
    public Builder name(@Nullable String name) {
      this.name = name;
      return this;
    }

    @NonNull
    @Deprecated
    public Builder category(@Nullable String category) {
      this.category = category;
      return this;
    }

    @NonNull
    public Builder properties(@NonNull Map<String, ?> properties) {
      assertNotNull(properties, "properties");
      this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
      return this;
    }

    @Override
    protected ScreenPayload realBuild(
        @NonNull String messageId,
        @NonNull Date timestamp,
        @NonNull Map<String, Object> context,
        @NonNull Map<String, Object> integrations,
        @Nullable String userId,
        @NonNull String anonymousId) {
      if (isNullOrEmpty(name) && isNullOrEmpty(category)) {
        throw new NullPointerException("either name or category is required");
      }

      Map<String, Object> properties = this.properties;
      if (isNullOrEmpty(properties)) {
        properties = Collections.emptyMap();
      }

      return new ScreenPayload(
          messageId,
          timestamp,
          context,
          integrations,
          userId,
          anonymousId,
          name,
          category,
          properties);
    }

    @Override
    Builder self() {
      return this;
    }
  }
}
