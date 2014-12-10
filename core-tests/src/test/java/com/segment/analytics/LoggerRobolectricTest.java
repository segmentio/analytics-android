/*
 * Copyright 2014 Prateek Srivastava
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

package com.segment.analytics;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class LoggerRobolectricTest {
  static final String TEST_OWNER = "Test";

  @Test public void disabledLogger() throws Exception {
    Logger logger = new Logger(false);

    logger.debug(TEST_OWNER, "foo", "bar", "qaz", "qux");
    logger.error(TEST_OWNER, "foo", "bar", new Throwable(), "qaz", "qux");
    List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).isEmpty();
  }

  @Test public void testDebugLogWithNullFormat() throws Exception {
    Logger logger = new Logger(true);
    logger.debug(TEST_OWNER, "foo", "bar", null, "qux");
    List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).msg).isEqualTo(
        "Test                 foo          bar                                  {null}");
  }

  @Test public void testDebugLogWithFormat() throws Exception {
    Logger logger = new Logger(true);
    logger.debug(TEST_OWNER, "foo", "bar", "qaz", "qux");
    List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).msg).isEqualTo(
        "Test                 foo          bar                                  {qaz}");
  }
}
