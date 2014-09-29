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

package com.segment.analytics;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Options let you control behaviour for a specific analytics call, including setting a custom
 * timestamp and disabling integrations on demand.
 */
public class Options {
  public static final String ALL_INTEGRATIONS_KEY = "all";

  private Date timestamp;
  private final Map<String, Boolean> integrations; // passed in by the user

  public Options() {
    integrations = new LinkedHashMap<String, Boolean>();
    integrations.put(ALL_INTEGRATIONS_KEY, true);
  }

  /**
   * Sets whether this call will be sent to the target integration.
   * <p/>
   * By default, all integrations are sent a payload. You can disable specific payloads. For
   * instance, <code>options.setIntegration("Google Analytics", false).setIntegration("Countly",
   * false)</code> will send the event to all integrations except Google Analytic and Countly. If
   * you want to enable only specific integrations, first override the defaults and then enable
   * specific integrations. For example, <code>options.setIntegration(Options.ALL_INTEGRATIONS_KEY,
   * false).setIntegration("Countly", true).setIntegration("Google Analytics", true)</code> will
   * only send events to Countly and Google Analytics.
   *
   * @param integrationKey The integration key
   * @param enabled <code>true</code> for enabled, <code>false</code> for disabled
   * @return This options object for chaining
   */
  public Options setIntegration(String integrationKey, boolean enabled) {
    integrations.put(integrationKey, enabled);
    return this;
  }

  // todo: hide from public API
  public Map<String, Boolean> integrations() {
    return Collections.unmodifiableMap(integrations);
  }

  /**
   * Sets the timestamp of when an analytics call occurred. The timestamp is primarily used for
   * historical imports or if this event happened in the past. The timestamp is not required, and
   * if it's not provided, our servers will timestamp the call as if it just happened.
   *
   * @param timestamp The time when this event happened
   * @return This options object for chaining
   */
  public Options setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  Date timestamp() {
    return timestamp;
  }
}
