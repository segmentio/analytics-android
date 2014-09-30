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

import android.content.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.isNullOrEmpty;
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

  @Test public void returnsConnectedIfMissingPermission() throws Exception {
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    assertThat(isConnected(context)).isTrue();
  }
}