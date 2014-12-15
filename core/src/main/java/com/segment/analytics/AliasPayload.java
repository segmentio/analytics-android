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

class AliasPayload extends BasePayload {
  /**
   * The previous ID for the user that you want to alias from, that you previously called identify
   * with as their User ID.
   */
  private static final String PREVIOUS_ID_KEY = "previousId";

  AliasPayload(AnalyticsContext context, Options options, String previousId) {
    super(Type.alias, context, options);
    put(PREVIOUS_ID_KEY, previousId);
  }

  String previousId() {
    return getString(PREVIOUS_ID_KEY);
  }

  @Override public void run(AbstractIntegration integration) {
    if (isIntegrationEnabledInPayload(integration)) integration.alias(this);
  }

  @Override public String toString() {
    return "AliasPayload{userId=\"" + userId() + ",previousId=\"" + previousId() + "\"}";
  }
}
