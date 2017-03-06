package com.segment.analytics;

import com.segment.analytics.integrations.BasePayload;
import java.util.List;

class RealInterceptorChain implements Interceptor.Chain {

  private int index;
  private final BasePayload payload;
  private final List<Interceptor> interceptors;
  private final Analytics analytics;

  RealInterceptorChain(
      int index, BasePayload payload, List<Interceptor> interceptors, Analytics analytics) {
    this.index = index;
    this.payload = payload;
    this.interceptors = interceptors;
    this.analytics = analytics;
  }

  @Override
  public BasePayload payload() {
    return payload;
  }

  @Override
  public void proceed(BasePayload payload) {
    // If there's another interceptor in the chain, call that.
    if (index < interceptors.size()) {
      Interceptor.Chain chain =
          new RealInterceptorChain(index + 1, payload, interceptors, analytics);
      interceptors.get(index).intercept(chain);
      return;
    }

    // No more interceptors.
    analytics.run(payload);
  }
}
