package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.robolectric.annotation.Config.NONE;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class CartographerTest {

  private Cartographer cartographer;

  @Before
  public void setUp() {
    cartographer = new Cartographer.Builder().lenient(false).prettyPrint(true).build();
  }

  @Test
  public void encodesPrimitives() throws IOException {
    Map<String, Object> map =
        ImmutableMap.<String, Object>builder()
            .put("byte", (byte) 32)
            .put("boolean", true)
            .put("short", (short) 100)
            .put("int", 1)
            .put("long", 43L)
            .put("float", 23f)
            .put("double", Math.PI)
            .put("char", 'a')
            .put("String", "string")
            .build();

    assertThat(cartographer.toJson(map))
        .isEqualTo(
            "{\n"
                + "  \"byte\": 32,\n"
                + "  \"boolean\": true,\n"
                + "  \"short\": 100,\n"
                + "  \"int\": 1,\n"
                + "  \"long\": 43,\n"
                + "  \"float\": 23.0,\n"
                + "  \"double\": 3.141592653589793,\n"
                + "  \"char\": \"a\",\n"
                + "  \"String\": \"string\"\n"
                + "}");
  }

  @Test
  public void decodesPrimitives() throws IOException {
    String json =
        "{\n"
            + "  \"byte\": 32,\n"
            + "  \"boolean\": true,\n"
            + "  \"short\": 100,\n"
            + "  \"int\": 1,\n"
            + "  \"long\": 43,\n"
            + "  \"float\": 23.0,\n"
            + "  \"double\": 3.141592653589793,\n"
            + "  \"char\": \"a\",\n"
            + "  \"String\": \"string\"\n"
            + "}";

    Map<String, Object> map = cartographer.fromJson(json);

    assertThat(map)
        .hasSize(9)
        .contains(MapEntry.entry("byte", 32.0))
        .contains(MapEntry.entry("boolean", true))
        .contains(MapEntry.entry("short", 100.0))
        .contains(MapEntry.entry("int", 1.0))
        .contains(MapEntry.entry("long", 43.0))
        .contains(MapEntry.entry("float", 23.0))
        .contains(MapEntry.entry("double", Math.PI))
        .contains(MapEntry.entry("char", "a"))
        .contains(MapEntry.entry("String", "string"));
  }

  @Test
  public void prettyPrintDisabled() throws IOException {
    Cartographer cartographer = new Cartographer.Builder().prettyPrint(false).build();
    Map<String, Object> map =
        ImmutableMap.<String, Object>builder()
            .put(
                "a",
                ImmutableMap.<String, Object>builder()
                    .put(
                        "b",
                        ImmutableMap.<String, Object>builder()
                            .put(
                                "c",
                                ImmutableMap.<String, Object>builder()
                                    .put(
                                        "d",
                                        ImmutableMap.<String, Object>builder()
                                            .put("e", "f")
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    assertThat(cartographer.toJson(map)) //
        .isEqualTo("{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":\"f\"}}}}}");
  }

  @Test
  public void encodesNestedMaps() throws IOException {
    Map<String, Object> map =
        ImmutableMap.<String, Object>builder()
            .put(
                "a",
                ImmutableMap.<String, Object>builder()
                    .put(
                        "b",
                        ImmutableMap.<String, Object>builder()
                            .put(
                                "c",
                                ImmutableMap.<String, Object>builder()
                                    .put(
                                        "d",
                                        ImmutableMap.<String, Object>builder()
                                            .put("e", "f")
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    assertThat(cartographer.toJson(map))
        .isEqualTo(
            "{\n"
                + "  \"a\": {\n"
                + "    \"b\": {\n"
                + "      \"c\": {\n"
                + "        \"d\": {\n"
                + "          \"e\": \"f\"\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void decodesNestedMaps() throws IOException {
    String json =
        "{\n"
            + "  \"a\": {\n"
            + "    \"b\": {\n"
            + "      \"c\": {\n"
            + "        \"d\": {\n"
            + "          \"e\": \"f\"\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    Map<String, Object> map = cartographer.fromJson(json);
    Map<String, Object> expected =
        ImmutableMap.<String, Object>builder()
            .put(
                "a",
                ImmutableMap.<String, Object>builder()
                    .put(
                        "b",
                        ImmutableMap.<String, Object>builder()
                            .put(
                                "c",
                                ImmutableMap.<String, Object>builder()
                                    .put(
                                        "d",
                                        ImmutableMap.<String, Object>builder()
                                            .put("e", "f")
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void encodesArraysWithLists() throws IOException {
    Map<String, Object> map =
        ImmutableMap.<String, Object>builder().put("a", Arrays.asList("b", "c", "d")).build();

    assertThat(cartographer.toJson(map))
        .isEqualTo(
            "{\n" //
                + "  \"a\": [\n" //
                + "    \"b\",\n" //
                + "    \"c\",\n" //
                + "    \"d\"\n" //
                + "  ]\n" //
                + "}");
  }

  @Test
  public void decodesArraysWithLists() throws IOException {
    String json =
        "{\n" //
            + "  \"a\": [\n" //
            + "    \"b\",\n" //
            + "    \"c\",\n" //
            + "    \"d\"\n" //
            + "  ]\n" //
            + "}";

    Map<String, Object> expected =
        ImmutableMap.<String, Object>builder().put("a", Arrays.asList("b", "c", "d")).build();

    assertThat(cartographer.fromJson(json)).isEqualTo(expected);
  }

  @Test
  public void encodesArraysWithArrays() throws IOException {
    Map<String, Object> map =
        ImmutableMap.<String, Object>builder().put("a", new String[] {"b", "c", "d"}).build();

    assertThat(cartographer.toJson(map))
        .isEqualTo(
            "{\n" //
                + "  \"a\": [\n" //
                + "    \"b\",\n" //
                + "    \"c\",\n" //
                + "    \"d\"\n" //
                + "  ]\n" //
                + "}");
  }

  @Test
  public void encodesPrimitiveArrays() throws IOException {
    // Exercise a bug where primitive arrays would throw an IOException.
    // https://github.com/segmentio/analytics-android/issues/507
    Map<String, Object> map =
        ImmutableMap.<String, Object>builder().put("a", new int[] {1, 2}).build();

    assertThat(cartographer.toJson(map))
        .isEqualTo("{\n" + "  \"a\": [\n" + "    1,\n" + "    2\n" + "  ]\n" + "}");
  }

  @Test
  public void decodesArraysWithArraysAsLists() throws IOException {
    String json =
        "{\n" //
            + "  \"a\": [\n" //
            + "    \"b\",\n" //
            + "    \"c\",\n" //
            + "    \"d\"\n" //
            + "  ]\n" //
            + "}";

    Map<String, Object> expected =
        ImmutableMap.<String, Object>builder().put("a", Arrays.asList("b", "c", "d")).build();

    assertThat(cartographer.fromJson(json)).isEqualTo(expected);
  }

  @Test
  public void encodesArrayOfMap() throws IOException {
    Map<String, Object> map =
        ImmutableMap.<String, Object>builder()
            .put(
                "a",
                Arrays.<ImmutableMap>asList(
                    ImmutableMap.<String, Object>builder().put("b", "c").build(),
                    ImmutableMap.<String, Object>builder().put("b", "d").build(),
                    ImmutableMap.<String, Object>builder().put("b", "e").build()))
            .build();

    assertThat(cartographer.toJson(map))
        .isEqualTo(
            "{\n"
                + "  \"a\": [\n"
                + "    {\n"
                + "      \"b\": \"c\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"b\": \"d\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"b\": \"e\"\n"
                + "    }\n"
                + "  ]\n"
                + "}");
  }

  @Test
  public void decodesArrayOfMap() throws IOException {
    String json =
        "{\n"
            + "  \"a\": [\n"
            + "    {\n"
            + "      \"b\": \"c\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"b\": \"d\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"b\": \"e\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    Map<String, Object> expected =
        ImmutableMap.<String, Object>builder()
            .put(
                "a",
                Arrays.<ImmutableMap>asList(
                    ImmutableMap.<String, Object>builder().put("b", "c").build(),
                    ImmutableMap.<String, Object>builder().put("b", "d").build(),
                    ImmutableMap.<String, Object>builder().put("b", "e").build()))
            .build();

    assertThat(cartographer.fromJson(json)).isEqualTo(expected);
  }

  @Test
  public void disallowsEncodingNullMap() throws IOException {
    try {
      cartographer.toJson(null, new StringWriter());
      fail("null map should throw Exception");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("map == null");
    }
  }

  @Test
  public void disallowsEncodingToNullWriter() throws IOException {
    try {
      cartographer.toJson(new LinkedHashMap<Object, Object>(), null);
      fail("null writer should throw Exception");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("writer == null");
    }
  }

  @Test
  public void disallowsDecodingNullReader() throws IOException {
    try {
      cartographer.fromJson((Reader) null);
      fail("null map should throw Exception");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("reader == null");
    }
  }

  @Test
  public void disallowsDecodingNullString() throws IOException {
    try {
      cartographer.fromJson((String) null);
      fail("null map should throw Exception");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("json == null");
    }
  }

  @Test
  public void disallowsDecodingEmptyString() throws IOException {
    try {
      cartographer.fromJson("");
      fail("null map should throw Exception");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("json empty");
    }
  }

  @Test
  public void encodesNumberMax() throws IOException {
    StringWriter writer = new StringWriter();
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put("byte", Byte.MAX_VALUE);
    map.put("short", Short.MAX_VALUE);
    map.put("int", Integer.MAX_VALUE);
    map.put("long", Long.MAX_VALUE);
    map.put("float", Float.MAX_VALUE);
    map.put("double", Double.MAX_VALUE);
    map.put("char", Character.MAX_VALUE);

    cartographer.toJson(map, writer);

    assertThat(writer.toString())
        .isEqualTo(
            "{\n"
                + "  \"byte\": 127,\n"
                + "  \"short\": 32767,\n"
                + "  \"int\": 2147483647,\n"
                + "  \"long\": 9223372036854775807,\n"
                + "  \"float\": 3.4028235E38,\n"
                + "  \"double\": 1.7976931348623157E308,\n"
                + "  \"char\": \"\uFFFF\"\n"
                + "}");
  }

  @Test
  public void encodesNumberMin() throws IOException {
    StringWriter writer = new StringWriter();
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put("byte", Byte.MIN_VALUE);
    map.put("short", Short.MIN_VALUE);
    map.put("int", Integer.MIN_VALUE);
    map.put("long", Long.MIN_VALUE);
    map.put("float", Float.MIN_VALUE);
    map.put("double", Double.MIN_VALUE);
    map.put("char", Character.MIN_VALUE);

    cartographer.toJson(map, writer);

    assertThat(writer.toString())
        .isEqualTo(
            "{\n"
                + "  \"byte\": -128,\n"
                + "  \"short\": -32768,\n"
                + "  \"int\": -2147483648,\n"
                + "  \"long\": -9223372036854775808,\n"
                + "  \"float\": 1.4E-45,\n"
                + "  \"double\": 4.9E-324,\n"
                + "  \"char\": \"\\u0000\"\n"
                + "}");
  }

  @Test
  public void encodesLargeDocuments() throws IOException {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < 100; i++) {
      map.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }
    StringWriter writer = new StringWriter();
    cartographer.toJson(map, writer);
  }
}
