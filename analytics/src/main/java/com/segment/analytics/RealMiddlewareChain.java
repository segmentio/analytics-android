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

import android.support.annotation.NonNull;
import com.segment.analytics.integrations.BasePayload;
import java.util.List;

class RealMiddlewareChain implements Middleware.Chain {

  private int index;
  private final @NonNull BasePayload payload;
  private final @NonNull List<Middleware> middlewares;
  private final @NonNull Analytics analytics;

  RealMiddlewareChain(
      int index,
      @NonNull BasePayload payload,
      @NonNull List<Middleware> middlewares,
      @NonNull Analytics analytics) {
    this.index = index;
    this.payload = payload;
    this.middlewares = middlewares;
    this.analytics = analytics;
  }

  @Override
  public BasePayload payload() {
    return payload;
  }

  @Override
  public void proceed(BasePayload payload) {
    // If there's another middleware in the chain, call that.
    if (index < middlewares.size()) {
      Middleware.Chain chain = new RealMiddlewareChain(index + 1, payload, middlewares, analytics);
      middlewares.get(index).intercept(chain);
      return;
    }

    // No more interceptors.
    analytics.run(payload);
  }
}
