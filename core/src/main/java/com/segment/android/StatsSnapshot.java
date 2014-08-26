package com.segment.android;

public class StatsSnapshot {
  public final long timestamp;
  public final long eventCount; // Number of events we've tracked any event
  public final long flushCount; // Number of times we've flushed to the server
  public final long integrationEventCount;
  // Number of times we've told an integration to do an event, including flushes
  public final long integrationEventTime;
  public final long averageIntegrationEventTime;

  public StatsSnapshot(long timestamp, long eventCount, long flushCount, long integrationEventCount,
      long integrationEventTime) {
    this.timestamp = timestamp;
    this.eventCount = eventCount;
    this.flushCount = flushCount;
    this.integrationEventCount = integrationEventCount;
    this.integrationEventTime = integrationEventTime;
    averageIntegrationEventTime = integrationEventTime / integrationEventCount;
  }

  @Override public String toString() {
    return "StatsSnapshot{"
        + "timestamp="
        + timestamp
        + ", eventCount="
        + eventCount
        + ", flushCount="
        + flushCount
        + ", integrationEventCount="
        + integrationEventCount
        + ", integrationEventTime="
        + integrationEventTime
        + ", averageIntegrationEventTime="
        + averageIntegrationEventTime
        + '}';
  }
}
