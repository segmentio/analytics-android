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
import com.segment.analytics.core.tests.BuildConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.internal.Utils.TAG;
import static com.segment.analytics.internal.Utils.debug;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class UtilsTest {

  @Mock Context context;

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void emptyString() throws Exception {
    assertThat(isNullOrEmpty((String) null)).isTrue();
    assertThat(isNullOrEmpty("")).isTrue();
    assertThat(isNullOrEmpty("    ")).isTrue();
    assertThat(isNullOrEmpty("  a  ")).isFalse();
    assertThat(isNullOrEmpty("a")).isFalse();
  }

  @Test public void emptyMap() throws Exception {
    assertThat(isNullOrEmpty((Map) null)).isTrue();
    Map<String, Object> map = new LinkedHashMap<>(20);
    assertThat(isNullOrEmpty(map)).isTrue();
    map.put("foo", "bar");
    assertThat(isNullOrEmpty(map)).isFalse();
    map.clear();
    assertThat(isNullOrEmpty(map)).isTrue();
  }

  @Test public void emptyCollections() throws Exception {
    assertThat(isNullOrEmpty((Collection) null)).isTrue();
    Collection<String> collection = new ArrayList<>();
    assertThat(isNullOrEmpty(collection)).isTrue();
    collection.add("foo");
    assertThat(isNullOrEmpty(collection)).isFalse();
    collection.clear();
    assertThat(isNullOrEmpty(collection)).isTrue();
  }

  @Test public void debugMessagesShowInLog() throws Exception {
    debug("some message with an %s", "argument");
    List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).containsExactly(new LogItemBuilder() //
        .type(Log.DEBUG).msg("some message with an argument").build());
  }

  @Test public void errorMessagesShowInLog() throws Exception {
    Throwable throwable = new AssertionError("testing");
    error(throwable, "some error occurred for %s", "foo");

    List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).containsExactly(new LogItemBuilder() //
        .type(Log.ERROR).throwable(throwable).msg("some error occurred for foo").build());
  }

  @Test public void returnsConnectedIfMissingPermission() throws Exception {
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    assertThat(isConnected(context)).isTrue();
  }

  static class LogItemBuilder {

    private int type;
    private String tag = TAG; // will be the default tag unless explicitly overridden
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
