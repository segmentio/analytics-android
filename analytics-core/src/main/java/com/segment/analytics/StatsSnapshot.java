package com.segment.analytics;

/** Value class for stats about an {@link Analytics} client. */
public class StatsSnapshot {
  public final long timestamp;
  public final long flushCount;
  public final long flushEventCount;
  public final long integrationOperationCount;
  public final long integrationOperationDuration;
  public final long integrationOperationAverageDuration;

  public StatsSnapshot(long timestamp, long flushCount, long flushEventCount,
      long integrationOperationCount, long integrationOperationDuration) {
    this.timestamp = timestamp;
    this.flushCount = flushCount;
    this.flushEventCount = flushEventCount;
    this.integrationOperationCount = integrationOperationCount;
    this.integrationOperationDuration = integrationOperationDuration;
    integrationOperationAverageDuration = integrationOperationDuration / integrationOperationCount;
  }

  @Override public String toString() {
    return "StatsSnapshot{"
        + "timestamp="
        + timestamp
        +
        ", flushCount="
        + flushCount
        +
        ", flushEventCount="
        + flushEventCount
        +
        ", integrationOperationCount="
        + integrationOperationCount
        +
        ", integrationOperationDuration="
        + integrationOperationDuration
        + ", integrationOperationAverageDuration="
        + integrationOperationAverageDuration
        +
        '}';
  }
}
