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

class IdentifyPayload extends BasePayload {
  /**
   * A dictionary of traits you know about a user, for example email or name. We have a collection
   * of special traits that we recognize with semantic meaning, which you should always use when
   * recording that information. You can also add any custom traits that are specific to your
   * project to the dictionary, like friendCount or subscriptionType.
   */
  private static final String TRAITS_KEY = "traits";

  IdentifyPayload(String anonymousId, AnalyticsContext context, String userId, Traits traits,
      Options options) {
    super(Type.identify, anonymousId, context, userId, options);
    put(TRAITS_KEY, traits);
  }

  Traits traits() {
    return getValueMap(TRAITS_KEY, Traits.class);
  }

  @Override public void run(AbstractIntegration integration) {
    if (isIntegrationEnabledInPayload(integration)) integration.identify(this);
  }

  @Override public String toString() {
    return "IdentifyPayload{\"userId=\"" + userId() + "\"," + traits() + '}';
  }
}
