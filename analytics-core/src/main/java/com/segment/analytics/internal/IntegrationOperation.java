package com.segment.analytics.internal;

/** Abstraction for a task that a {@link AbstractIntegration} can execute. */
public interface IntegrationOperation {
  /** Run this operation on the given integration. */
  void run(AbstractIntegration integration);

  /** Return a unique ID to identify this operation. */
  String id();
}
