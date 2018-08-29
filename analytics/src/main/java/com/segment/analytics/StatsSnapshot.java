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

  public StatsSnapshot(
      long timestamp,
      long flushCount,
      long flushEventCount,
      long integrationOperationCount,
      long integrationOperationDuration,
      Map<String, Long> integrationOperationDurationByIntegration) {
    this.timestamp = timestamp;
    this.flushCount = flushCount;
    this.flushEventCount = flushEventCount;
    this.integrationOperationCount = integrationOperationCount;
    this.integrationOperationDuration = integrationOperationDuration;
    this.integrationOperationAverageDuration =
        (integrationOperationCount == 0)
            ? 0
            : ((float) integrationOperationDuration / integrationOperationCount);
    this.integrationOperationDurationByIntegration = integrationOperationDurationByIntegration;
  }

  @Override
  public String toString() {
    return "StatsSnapshot{"
        + "timestamp="
        + timestamp
        + ", flushCount="
        + flushCount
        + ", flushEventCount="
        + flushEventCount
        + ", integrationOperationCount="
        + integrationOperationCount
        + ", integrationOperationDuration="
        + integrationOperationDuration
        + ", integrationOperationAverageDuration="
        + integrationOperationAverageDuration
        + ", integrationOperationDurationByIntegration="
        + integrationOperationDurationByIntegration
        + '}';
  }
}
