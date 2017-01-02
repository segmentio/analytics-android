/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment, Inc.
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

import android.util.Pair;
import com.segment.analytics.core.tests.BuildConfig;
import java.io.IOException;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = NONE)
public class StatsTest {

  Stats stats;

  @Before public void setUp() {
    stats = new Stats();
  }

  @Test public void performFlush() throws IOException {
    stats.performFlush(4);
    assertThat(stats.flushCount).isEqualTo(1);
    assertThat(stats.flushEventCount).isEqualTo(4);

    stats.performFlush(10);
    assertThat(stats.flushCount).isEqualTo(2);
    assertThat(stats.flushEventCount).isEqualTo(14);
  }

  @Test public void performIntegrationOperation() throws IOException {
    stats.performIntegrationOperation(new Pair<>("foo", 43L));
    assertThat(stats.integrationOperationCount).isEqualTo(1);
    assertThat(stats.integrationOperationDuration).isEqualTo(43L);
    assertThat(stats.integrationOperationDurationByIntegration) //
        .containsExactly(MapEntry.entry("foo", 43L));

    stats.performIntegrationOperation(new Pair<>("bar", 2L));
    assertThat(stats.integrationOperationCount).isEqualTo(2);
    assertThat(stats.integrationOperationDuration).isEqualTo(45L);
    assertThat(stats.integrationOperationDurationByIntegration).hasSize(2)
        .contains(MapEntry.entry("bar", 2L));

    stats.performIntegrationOperation(new Pair<>("bar", 19L));
    assertThat(stats.integrationOperationCount).isEqualTo(3);
    assertThat(stats.integrationOperationDuration).isEqualTo(64L);
    assertThat(stats.integrationOperationDurationByIntegration).hasSize(2)
        .contains(MapEntry.entry("bar", 21L));
  }

  @Test public void createSnapshot() throws IOException {
    stats.performFlush(1);
    stats.performFlush(1);
    stats.performFlush(2);
    stats.performFlush(3);
    stats.performFlush(5);
    stats.performFlush(8);
    stats.performFlush(13);
    stats.performFlush(21);

    stats.performIntegrationOperation(new Pair<>("foo", 1L));
    stats.performIntegrationOperation(new Pair<>("foo", 1L));
    stats.performIntegrationOperation(new Pair<>("foo", 1L));
    stats.performIntegrationOperation(new Pair<>("foo", 1L));
    stats.performIntegrationOperation(new Pair<>("foo", 1L));
    stats.performIntegrationOperation(new Pair<>("foo", 1L));

    stats.performIntegrationOperation(new Pair<>("bar", 2L));
    stats.performIntegrationOperation(new Pair<>("bar", 2L));
    stats.performIntegrationOperation(new Pair<>("bar", 2L));
    stats.performIntegrationOperation(new Pair<>("bar", 2L));

    StatsSnapshot snapshot = stats.createSnapshot();
    assertThat(snapshot.flushCount).isEqualTo(8);
    assertThat(snapshot.flushEventCount).isEqualTo(54);
    assertThat(snapshot.integrationOperationCount).isEqualTo(10);
    assertThat(snapshot.integrationOperationDuration).isEqualTo(14L);
    assertThat(snapshot.integrationOperationAverageDuration).isEqualTo(1.4f);
    assertThat(snapshot.integrationOperationDurationByIntegration).hasSize(2)
        .containsEntry("foo", 6L)
        .containsEntry("bar", 8L);

    try {
      snapshot.integrationOperationDurationByIntegration.put("qaz", 10L);
      fail("Map instance should be immutable.");
    } catch (UnsupportedOperationException ignored) {
    }
  }

  @Test public void createEmptySnapshot() throws IOException {
    StatsSnapshot snapshot = stats.createSnapshot();

    assertThat(snapshot.timestamp).isNotZero();
    assertThat(snapshot.flushCount).isZero();
    assertThat(snapshot.flushEventCount).isZero();
    assertThat(snapshot.integrationOperationCount).isZero();
    assertThat(snapshot.integrationOperationDuration).isZero();
    assertThat(snapshot.integrationOperationAverageDuration).isZero();
    assertThat(snapshot.integrationOperationDurationByIntegration).isEmpty();
  }
}
