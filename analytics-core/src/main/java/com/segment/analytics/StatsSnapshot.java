package com.segment.analytics;

import java.util.Map;

/** Represents all stats for a {@link Analytics} instance at a single point in time. */
public class StatsSnapshot {
  /** The time at which the snapshot was created. */
  public final long timestamp;
  /** Number of times we've flushed events to our servers. */
  public final long flushCount;
  /** Number of events we've flushed to our servers. */
  public final long flushEventCount;
  /**
   * Number of operations sent to all bundled integrations, including lifecycle events and flushes.
   */
  public final long integrationOperationCount;
  /**
   * Total time to run operations on all bundled integrations, including lifecycle events and
   * flushes.
   */
  public final long integrationOperationDuration;
  /**
   * Average time to run operations on all bundled integrations, including lifecycle events and
   * flushes.
   */
  public final float integrationOperationAverageDuration;
  /** Total time to run operations, including lifecycle events and flushes, by integration. */
  public final Map<String, Long> integrationOperationDurationByIntegration;

  public StatsSnapshot(long timestamp, long flushCount, long flushEventCount,
      long integrationOperationCount, long integrationOperationDuration,
      Map<String, Long> integrationOperationDurationByIntegration) {
    this.timestamp = timestamp;
    this.flushCount = flushCount;
    this.flushEventCount = flushEventCount;
    this.integrationOperationCount = integrationOperationCount;
    this.integrationOperationDuration = integrationOperationDuration;
    this.integrationOperationAverageDuration = (integrationOperationCount == 0) ? 0
        : ((float) integrationOperationDuration / integrationOperationCount);
    this.integrationOperationDurationByIntegration = integrationOperationDurationByIntegration;
  }

  @Override public String toString() {
    return "StatsSnapshot{"
        + "timestamp="
        + timestamp
        + ", flushCount="
        + flushCount
        + ", flushEventCount="
        + flushEventCount
        +
        ", integrationOperationCount="
        + integrationOperationCount
        +
        ", integrationOperationDuration="
        + integrationOperationDuration
        +
        ", integrationOperationAverageDuration="
        + integrationOperationAverageDuration
        +
        ", integrationOperationDurationByIntegration="
        + integrationOperationDurationByIntegration
        +
        '}';
  }
}
