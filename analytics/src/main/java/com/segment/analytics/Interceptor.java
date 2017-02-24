package com.segment.analytics;

import com.segment.analytics.integrations.BasePayload;

/** Intercept every message after it is built to process it further. */
public interface Interceptor {

  /** Called for every message. This will be called on the same thread the request was made. */
  void intercept(Chain chain);

  interface Chain {

    BasePayload payload();

    void proceed(BasePayload payload);
  }
}
