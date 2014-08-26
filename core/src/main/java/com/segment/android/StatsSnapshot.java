package com.segment.android;

public class StatsSnapshot {
  public final long timestamp;
  public final long eventCount; // Number of events we've tracked
  public final long flushCount; // Number of times we've flushed to the server

  public StatsSnapshot(long timestamp, long eventCount, long flushCount) {
    this.timestamp = timestamp;
    this.eventCount = eventCount;
    this.flushCount = flushCount;
  }

  @Override public String toString() {
    return "StatsSnapshot{"
        + "timestamp="
        + timestamp
        + ", eventCount="
        + eventCount
        + ", flushCount="
        + flushCount
        + '}';
  }
}
