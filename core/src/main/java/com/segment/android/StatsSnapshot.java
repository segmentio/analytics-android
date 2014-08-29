package com.segment.android;

public class StatsSnapshot {
  public final long timestamp;
  public final long identifyCount;
  public final long groupCount;
  public final long trackCount;
  public final long screenCount;
  public final long flushCount;
  public final long flushEventCount;
  public final long integrationOperationCount;
  public final long integrationOperationDuration;
  public final long integrationOperationAverageDuration;
  public final long totalEventsCount;

  public StatsSnapshot(long timestamp, long identifyCount, long groupCount, long trackCount,
      long screenCount, long flushCount, long flushEventCount, long integrationOperationCount,
      long integrationOperationDuration) {
    this.timestamp = timestamp;
    this.identifyCount = identifyCount;
    this.groupCount = groupCount;
    this.trackCount = trackCount;
    this.screenCount = screenCount;
    this.flushCount = flushCount;
    this.flushEventCount = flushEventCount;
    this.integrationOperationCount = integrationOperationCount;
    this.integrationOperationDuration = integrationOperationDuration;
    integrationOperationAverageDuration = integrationOperationDuration / integrationOperationCount;
    totalEventsCount = identifyCount + groupCount + trackCount + screenCount + flushCount;
  }

  @Override public String toString() {
    return "StatsSnapshot{"
        + "timestamp="
        + timestamp
        + ", identifyCount="
        + identifyCount
        + ", groupCount="
        + groupCount
        +
        ", trackCount="
        + trackCount
        +
        ", screenCount="
        + screenCount
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
        +
        ", integrationOperationAverageDuration="
        + integrationOperationAverageDuration
        +
        ", totalEventsCount="
        + totalEventsCount
        +
        '}';
  }
}
