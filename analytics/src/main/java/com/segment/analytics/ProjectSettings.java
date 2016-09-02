/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment, Inc.
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

import android.content.Context;
import com.segment.analytics.internal.Private;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

class ProjectSettings extends ValueMap {

  private static final String TIMESTAMP_KEY = "timestamp";
  private static final String PLAN_KEY = "plan";
  private static final String INTEGRATIONS_KEY = "integrations";
  private static final String TRACKING_PLAN_KEY = "track";

  static ProjectSettings create(Map<String, Object> map) {
    map.put(TIMESTAMP_KEY, System.currentTimeMillis());
    return new ProjectSettings(map);
  }

  @Private ProjectSettings(Map<String, Object> map) {
    super(unmodifiableMap(map));
  }

  long timestamp() {
    return getLong(TIMESTAMP_KEY, 0L);
  }

  ValueMap plan() {
    return getValueMap(PLAN_KEY);
  }

  ValueMap trackingPlan() {
    ValueMap plan = plan();
    if (plan == null) {
      return null;
    }
    return plan.getValueMap(TRACKING_PLAN_KEY);
  }

  ValueMap integrations() {
    return getValueMap(INTEGRATIONS_KEY);
  }

  static class Cache extends ValueMap.Cache<ProjectSettings> {

    // todo: remove. This is legacy behaviour from before we started namespacing the entire shared
    // preferences object and were namespacing keys instead.
    private static final String PROJECT_SETTINGS_CACHE_KEY_PREFIX = "project-settings-plan-";

    Cache(Context context, Cartographer cartographer, String tag) {
      super(context, cartographer, PROJECT_SETTINGS_CACHE_KEY_PREFIX + tag, tag,
          ProjectSettings.class);
    }

    @Override public ProjectSettings create(Map<String, Object> map) {
      return new ProjectSettings(map);
    }
  }
}
