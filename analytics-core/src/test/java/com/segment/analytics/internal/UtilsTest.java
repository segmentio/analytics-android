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

package com.segment.analytics.internal;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.internal.Utils.OWNER_MAIN;
import static com.segment.analytics.internal.Utils.OWNER_SEGMENT;
import static com.segment.analytics.internal.Utils.TAG;
import static com.segment.analytics.internal.Utils.VERB_DISPATCH;
import static com.segment.analytics.internal.Utils.VERB_ENQUEUE;
import static com.segment.analytics.internal.Utils.VERB_FLUSH;
import static com.segment.analytics.internal.Utils.debug;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.print;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class UtilsTest {
  @Mock Context context;

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void emptiness() throws Exception {
    assertThat(isNullOrEmpty((String) null)).isTrue();
    assertThat(isNullOrEmpty("")).isTrue();
    assertThat(isNullOrEmpty("    ")).isTrue();
    assertThat(isNullOrEmpty("  a  ")).isFalse();
    assertThat(isNullOrEmpty("a")).isFalse();

    assertThat(isNullOrEmpty((Map) null)).isTrue();
    Map<String, Object> map = new LinkedHashMap<String, Object>(20);
    assertThat(isNullOrEmpty(map)).isTrue();
    map.put("foo", "bar");
    assertThat(isNullOrEmpty(map)).isFalse();
    map.clear();
    assertThat(isNullOrEmpty(map)).isTrue();

    assertThat(isNullOrEmpty((Collection) null)).isTrue();
    Collection<String> collection = new ArrayList<String>();
    assertThat(isNullOrEmpty(collection)).isTrue();
    collection.add("foo");
    assertThat(isNullOrEmpty(collection)).isFalse();
    collection.clear();
    assertThat(isNullOrEmpty(collection)).isTrue();
  }

  @Test public void debugMessagesShowInLog() throws Exception {
    debug(OWNER_MAIN, VERB_DISPATCH, "foo", null, "bar");
    List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).containsExactly(new LogItemBuilder() //
        .type(Log.DEBUG)
        .msg("Main                 dispatch     foo                                  null, bar")
        .build());
  }

  @Test public void errorMessagesShowInLog() throws Exception {
    error(OWNER_MAIN, VERB_FLUSH, "foo", null, "bar", "baz");
    Throwable throwable = new AssertionError("testing");
    error(OWNER_SEGMENT, VERB_ENQUEUE, "qux", throwable);

    List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).hasSize(2)
        .contains(new LogItemBuilder().type(Log.ERROR)
            .msg("Main                 flush        foo                                  bar, baz")
            .build())
        .contains(new LogItemBuilder().type(Log.ERROR)
            .throwable(throwable)
            .msg("Segment              enqueue      qux                                  ")
            .build());
  }

  @Test public void printMessagesShowInLog() throws Exception {
    Throwable throwable = new AssertionError("testing");
    print(throwable, "foo");
    print("%s-%s", "bar", "baz");

    List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).hasSize(2)
        .contains(new LogItemBuilder().type(Log.ERROR).msg("foo").throwable(throwable).build())
        .contains(new LogItemBuilder().type(Log.DEBUG).msg("bar-baz").build());
  }

  @Test public void returnsConnectedIfMissingPermission() throws Exception {
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    assertThat(isConnected(context)).isTrue();
  }

  static class LogItemBuilder {
    private int type;
    private String tag = TAG; // will be the default tag unless explicitly overriden
    private String msg;
    private Throwable throwable;

    public LogItemBuilder type(int type) {
      this.type = type;
      return this;
    }

    public LogItemBuilder tag(String tag) {
      this.tag = tag;
      return this;
    }

    public LogItemBuilder msg(String msg) {
      this.msg = msg;
      return this;
    }

    public LogItemBuilder throwable(Throwable throwable) {
      this.throwable = throwable;
      return this;
    }

    public ShadowLog.LogItem build() {
      return new ShadowLog.LogItem(type, tag, msg, throwable);
    }
  }
}
