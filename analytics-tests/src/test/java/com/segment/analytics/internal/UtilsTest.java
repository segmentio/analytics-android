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

package com.segment.analytics.internal;

import android.content.Context;
import android.content.SharedPreferences;
import com.segment.analytics.core.tests.BuildConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.transform;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = NONE) public class UtilsTest {

  @Mock Context context;

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void emptyString() {
    assertThat(isNullOrEmpty((String) null)).isTrue();
    assertThat(isNullOrEmpty("")).isTrue();
    assertThat(isNullOrEmpty("    ")).isTrue();
    assertThat(isNullOrEmpty("  a  ")).isFalse();
    assertThat(isNullOrEmpty("a")).isFalse();
  }

  @Test public void emptyMap() {
    assertThat(isNullOrEmpty((Map) null)).isTrue();
    Map<String, Object> map = new LinkedHashMap<>(20);
    assertThat(isNullOrEmpty(map)).isTrue();
    map.put("foo", "bar");
    assertThat(isNullOrEmpty(map)).isFalse();
    map.clear();
    assertThat(isNullOrEmpty(map)).isTrue();
  }

  @Test public void emptyCollections() {
    assertThat(isNullOrEmpty((Collection) null)).isTrue();
    Collection<String> collection = new ArrayList<>();
    assertThat(isNullOrEmpty(collection)).isTrue();
    collection.add("foo");
    assertThat(isNullOrEmpty(collection)).isFalse();
    collection.clear();
    assertThat(isNullOrEmpty(collection)).isTrue();
  }

  @Test public void testTransform() {
    Map<String, Integer> in = new LinkedHashMap<>();
    in.put("a", 1);
    in.put("b", 2);
    in.put("c", 3);
    in.put("d", 4);

    Map<String, String> mapper = new LinkedHashMap<>();
    mapper.put("a", "$a");
    mapper.put("c", "");
    mapper.put("d", null);

    assertThat(transform(in, mapper)).containsExactly(MapEntry.entry("$a", 1),
        MapEntry.entry("b", 2));
  }

  @Test public void returnsConnectedIfMissingPermission() throws Exception {
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    assertThat(isConnected(context)).isTrue();
  }

  @Test public void nullableConcurrentHashMapPutIgnoresNulls() throws Exception {
    Map<String, String> map = new Utils.NullableConcurrentHashMap<>();
    map.put(null, null);
    map.put("foo", null);
    map.put(null, "bar");

    assertThat(map).isEmpty();
  }

  @Test public void nullableConcurrentHashMapPutAllIgnoresNulls() throws Exception {
    Map<String, String> values = new LinkedHashMap<>();
    values.put(null, null);
    values.put("foo", null);
    values.put(null, "bar");

    Map<String, String> map = new Utils.NullableConcurrentHashMap<>();
    map.putAll(values);
    assertThat(map).isEmpty();
  }

  @Test public void copySharedPreferences() {
    SharedPreferences src =
        RuntimeEnvironment.application.getSharedPreferences("src", Context.MODE_PRIVATE);
    src.edit().clear().apply();
    src.edit()
        .putBoolean("aBool", true)
        .putString("aString", "foo")
        .putInt("anInt", 2)
        .putFloat("aFloat", 3.14f)
        .putLong("aLong", 12345678910L)
        .putStringSet("aStringSet", new HashSet<>(Arrays.asList("foo", "bar")))
        .apply();

    SharedPreferences target =
        RuntimeEnvironment.application.getSharedPreferences("target", Context.MODE_PRIVATE);
    target.edit().clear().apply();

    Utils.copySharedPreferences(src, target);
    assertThat(target).contains("aBool", true)
        .contains("aString", "foo")
        .contains("anInt", 2)
        .contains("aFloat", 3.14f)
        .contains("aLong", 12345678910L)
        .contains("aStringSet", new HashSet<>(Arrays.asList("foo", "bar")));
  }
}
