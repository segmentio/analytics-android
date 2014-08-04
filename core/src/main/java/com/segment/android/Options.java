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

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class Options {
  private Calendar timestamp;
  private Set<String> disabledIntegrations = new LinkedHashSet<String>();

  /**
   * Sets whether this call will be sent to the target integration. For instance:
   * .disableIntegration("Google Analytics") will disable Google Analytics
   *
   * @param integration The integration name
   * @param enabled True for enabled
   * @return This options object for chaining
   */
  public Options disableIntegration(String integration) {
    disabledIntegrations.add(integration);
    return this;
  }

  Collection<String> getDisabledIntegrations() {
    return disabledIntegrations;
  }

  /**
   * Sets the timestamp of when an analytics call occurred. The timestamp is primarily used for
   * historical imports or if this event happened in the past. The timestamp is not required, and
   * if
   * its not provided, our servers will timestamp the call as if it just happened.
   *
   * @param timestamp The time when this event happened
   * @return This options object for chaining
   */
  public Options setTimestamp(Calendar timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public Calendar getTimestamp() {
    return timestamp;
  }
}
