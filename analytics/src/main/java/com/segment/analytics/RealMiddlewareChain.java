package com.segment.analytics;

import com.segment.analytics.integrations.BasePayload;
import java.util.List;

class RealMiddlewareChain implements Middleware.Chain {

  private int index;
  private final BasePayload payload;
  private final List<Middleware> middlewares;
  private final Analytics analytics;

  RealMiddlewareChain(
      int index, BasePayload payload, List<Middleware> middlewares, Analytics analytics) {
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
