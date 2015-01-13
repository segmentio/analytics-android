/*
 * Copyright 2015 Prateek Srivastava
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.segment.analytics.internal;

import android.util.Pair;
import com.segment.analytics.StatsSnapshot;
import java.io.IOException;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
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
