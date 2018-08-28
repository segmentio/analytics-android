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

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.FEATURE_TELEPHONY;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.transform;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import okio.Buffer;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class UtilsTest {

  @Mock Context context;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void emptyString() {
    assertThat(isNullOrEmpty((String) null)).isTrue();
    assertThat(isNullOrEmpty("")).isTrue();
    assertThat(isNullOrEmpty("    ")).isTrue();
    assertThat(isNullOrEmpty("  a  ")).isFalse();
    assertThat(isNullOrEmpty("a")).isFalse();
  }

  @Test
  public void emptyMap() {
    assertThat(isNullOrEmpty((Map) null)).isTrue();
    Map<String, Object> map = new LinkedHashMap<>(20);
    assertThat(isNullOrEmpty(map)).isTrue();
    map.put("foo", "bar");
    assertThat(isNullOrEmpty(map)).isFalse();
    map.clear();
    assertThat(isNullOrEmpty(map)).isTrue();
  }

  @Test
  public void emptyCollections() {
    assertThat(isNullOrEmpty((Collection) null)).isTrue();
    Collection<String> collection = new ArrayList<>();
    assertThat(isNullOrEmpty(collection)).isTrue();
    collection.add("foo");
    assertThat(isNullOrEmpty(collection)).isFalse();
    collection.clear();
    assertThat(isNullOrEmpty(collection)).isTrue();
  }

  @Test
  public void testTransform() {
    Map<String, Integer> in = new LinkedHashMap<>();
    in.put("a", 1);
    in.put("b", 2);
    in.put("c", 3);
    in.put("d", 4);

    Map<String, String> mapper = new LinkedHashMap<>();
    mapper.put("a", "$a");
    mapper.put("c", "");
    mapper.put("d", null);

    assertThat(transform(in, mapper))
        .containsExactly(MapEntry.entry("$a", 1), MapEntry.entry("b", 2));
  }

  @Test
  public void returnsConnectedIfMissingPermission() throws Exception {
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    assertThat(isConnected(context)).isTrue();
  }

  @Test
  public void nullableConcurrentHashMapPutIgnoresNulls() throws Exception {
    Map<String, String> map = new Utils.NullableConcurrentHashMap<>();
    map.put(null, null);
    map.put("foo", null);
    map.put(null, "bar");

    assertThat(map).isEmpty();
  }

  @Test
  public void nullableConcurrentHashMapPutAllIgnoresNulls() throws Exception {
    Map<String, String> values = new LinkedHashMap<>();
    values.put(null, null);
    values.put("foo", null);
    values.put(null, "bar");

    Map<String, String> map = new Utils.NullableConcurrentHashMap<>();
    map.putAll(values);
    assertThat(map).isEmpty();
  }

  @Test
  public void copySharedPreferences() {
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
    assertThat(target)
        .contains("aBool", true)
        .contains("aString", "foo")
        .contains("anInt", 2)
        .contains("aFloat", 3.14f)
        .contains("aLong", 12345678910L)
        .contains("aStringSet", new HashSet<>(Arrays.asList("foo", "bar")));
  }

  @Test
  public void newSet() {
    assertThat(Utils.newSet("foo", "bar")).containsOnly("foo", "bar");
  }

  @Test
  public void coerceToFloat() {
    assertThat(Utils.coerceToFloat("not a number", 1)).isEqualTo(1);
    assertThat(Utils.coerceToFloat(3.14f, 0)).isEqualTo(3.14f);
    assertThat(Utils.coerceToFloat(1, 0)).isEqualTo(1);
  }

  @Test
  public void hasPermission() {
    when(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET))
        .thenReturn(PERMISSION_DENIED);
    assertThat(Utils.hasPermission(context, Manifest.permission.INTERNET)).isFalse();

    when(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET))
        .thenReturn(PERMISSION_GRANTED);
    assertThat(Utils.hasPermission(context, Manifest.permission.INTERNET)).isTrue();
  }

  @Test
  public void hasFeature() {
    PackageManager packageManager = mock(PackageManager.class);
    when(context.getPackageManager()).thenReturn(packageManager);

    when(packageManager.hasSystemFeature(FEATURE_TELEPHONY)).thenReturn(false);
    assertThat(Utils.hasFeature(context, FEATURE_TELEPHONY)).isFalse();

    when(packageManager.hasSystemFeature(FEATURE_TELEPHONY)).thenReturn(true);
    assertThat(Utils.hasFeature(context, FEATURE_TELEPHONY)).isTrue();
  }

  @Test
  public void assertNotNullOrEmptyMap() {
    try {
      Utils.assertNotNullOrEmpty((Map) null, "foo");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("foo cannot be null or empty");
    }

    try {
      Utils.assertNotNullOrEmpty(ImmutableMap.of(), "bar");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("bar cannot be null or empty");
    }

    assertThat(Utils.assertNotNullOrEmpty(ImmutableMap.of("key", "val"), "foo"))
        .isEqualTo(ImmutableMap.of("key", "val"));
  }

  @Test
  public void assertNotNullOrEmptyText() {
    try {
      Utils.assertNotNullOrEmpty((String) null, "foo");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("foo cannot be null or empty");
    }

    try {
      Utils.assertNotNullOrEmpty("", "bar");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("bar cannot be null or empty");
    }

    assertThat(Utils.assertNotNullOrEmpty("foo", "bar")).isEqualTo("foo");
  }

  @Test
  public void assertNotNull() {
    try {
      Utils.assertNotNull(null, "foo");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("foo == null");
    }

    Object something = new Object();
    assertThat(Utils.assertNotNull(something, "foo")).isEqualTo(something);
  }

  @Test
  public void readFully() throws IOException {
    assertThat(Utils.readFully(Utils.buffer(new Buffer().writeUtf8("foo\nbar").inputStream())))
        .isEqualTo("foobar");
  }
}
