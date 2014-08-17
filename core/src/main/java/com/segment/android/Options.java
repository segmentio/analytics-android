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

import java.util.LinkedHashMap;
import java.util.Map;

public class Options {
  public static final String ALL_INTEGRATIONS = "all";

  private final Map<String, Boolean> integrations;
  private long timestamp;

  public Options() {
    integrations = new LinkedHashMap<String, Boolean>();
    integrations.put(ALL_INTEGRATIONS, true);
  }

  /**
   * Sets whether this call will be sent to the target integration.By default, all integrations are
   * sent a payload.
   * <p/>
   * You can disable integrations for specific payloads. For instance,
   * <code>options.setIntegration("Google Analytics", false).setIntegration("Countly",
   * false)</code> will send the event to all integrations except Google Analytic and Countly. If
   * you want to enable only specific integrations, first override the defaults and then enable
   * specific integrations. For example, <code>options.setIntegration(Options.ALL_INTEGRATIONS,
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

  Map<String, Boolean> getIntegrations() {
    return integrations; // todo: wrap in Collections.Unmodifiable?
  }

  /**
   * Sets the timestamp of when an analytics call occurred. The timestamp is primarily used for
   * historical imports or if this event happened in the past. By default, this is set to the
   * current time.
   *
   * @param timestamp The time when this event happened
   * @return This options object for chaining
   */
  public Options setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
