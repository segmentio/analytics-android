/*
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

package com.segment.android;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.fest.assertions.data.Index;
import org.fest.assertions.data.MapEntry;

import static org.fest.assertions.api.Assertions.assertThat;

public class JsonUtilsTest extends BaseAndroidTestCase {
  public void testSimpleJsonToMap() throws Exception {
    Map<String, Object> json = JsonUtils.toMap("{\n"
            + "  \"glossary\": {\n"
            + "    \"title\": \"example glossary\",\n"
            + "    \"GlossDiv\": {\n"
            + "      \"title\": \"S\",\n"
            + "      \"GlossList\": {\n"
            + "        \"GlossEntry\": {\n"
            + "          \"ID\": \"SGML\",\n"
            + "          \"SortAs\": \"SGML\",\n"
            + "          \"GlossTerm\": \"Standard Generalized Markup Language\",\n"
            + "          \"Acronym\": \"SGML\",\n"
            + "          \"Abbrev\": \"ISO 8879:1986\",\n"
            + "          \"GlossDef\": {\n"
            + "            \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n"
            + "            \"GlossSeeAlso\": [\n"
            + "              \"GML\",\n"
            + "              \"XML\"\n"
            + "            ]\n"
            + "          },\n"
            + "          \"GlossSee\": \"markup\"\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}"
    );

    assertThat(json).hasSize(1);
    assertThat(json).containsKey("glossary");

    Map<String, Object> glossary = (Map<String, Object>) json.get("glossary");
    assertThat(glossary).hasSize(2);
    assertThat(glossary).containsKey("title");
    assertThat((String) glossary.get("title")).isEqualTo("example glossary");
    assertThat(glossary).containsKey("GlossDiv");

    Map<String, Object> GlossDiv = (Map<String, Object>) glossary.get("GlossDiv");
    assertThat(GlossDiv).hasSize(2);
    assertThat(GlossDiv).containsKey("title");
    assertThat((String) GlossDiv.get("title")).isEqualTo("S");
    assertThat(GlossDiv).containsKey("GlossList");

    Map<String, Object> GlossList = (Map<String, Object>) GlossDiv.get("GlossList");
    assertThat(GlossList).hasSize(1);
    assertThat(GlossList).containsKey("GlossEntry");

    Map<String, Object> GlossEntry = (Map<String, Object>) GlossList.get("GlossEntry");
    assertThat(GlossEntry).hasSize(7);
    assertThat(GlossEntry).containsKey("ID")
        .containsKey("SortAs")
        .containsKey("GlossTerm")
        .containsKey("Acronym")
        .containsKey("Abbrev")
        .containsKey("GlossDef")
        .containsKey("GlossSee");

    Map<String, Object> GlossDef = (Map<String, Object>) GlossEntry.get("GlossDef");
    assertThat(GlossDef).hasSize(2);
    assertThat(GlossDef).containsKey("para").containsKey("GlossSeeAlso");

    List<Object> GlossSeeAlso = (List<Object>) GlossDef.get("GlossSeeAlso");
    assertThat(GlossSeeAlso).hasSize(2).contains("GML", "XML");
  }

  public void testArray() throws Exception {
    List<Object> json = JsonUtils.toList("[\n"
            + "        {\"id\": \"Open\"},\n"
            + "        {\"id\": \"OpenNew\", \"label\": \"Open New\"},\n"
            + "        null,\n"
            + "        {\"id\": \"ZoomIn\", \"label\": \"Zoom In\"},\n"
            + "        {\"id\": \"ZoomOut\", \"label\": \"Zoom Out\"},\n"
            + "        {\"id\": \"OriginalView\", \"label\": \"Original View\"},\n"
            + "        null,\n"
            + "        {\"id\": \"Quality\"},\n"
            + "        {\"id\": \"Pause\"},\n"
            + "        {\"id\": \"Mute\"},\n"
            + "        null,\n"
            + "        {\"id\": \"Find\", \"label\": \"Find...\"},\n"
            + "        {\"id\": \"FindAgain\", \"label\": \"Find Again\"},\n"
            + "        {\"id\": \"Copy\"},\n"
            + "        {\"id\": \"CopyAgain\", \"label\": \"Copy Again\"},\n"
            + "        {\"id\": \"CopySVG\", \"label\": \"Copy SVG\"},\n"
            + "        {\"id\": \"ViewSVG\", \"label\": \"View SVG\"},\n"
            + "        {\"id\": \"ViewSource\", \"label\": \"View Source\"},\n"
            + "        {\"id\": \"SaveAs\", \"label\": \"Save As\"},\n"
            + "        null,\n"
            + "        {\"id\": \"Help\"},\n"
            + "        {\"id\": \"About\", \"label\": \"About Adobe CVG Viewer...\"}\n"
            + "    ]"
    );

    assertThat(json).hasSize(22);
    assertThat(json).contains(null, Index.atIndex(2))
        .contains(null, Index.atIndex(6))
        .contains(null, Index.atIndex(10))
        .contains(null, Index.atIndex(19));
    Map<String, Object> item21 = new HashMap<String, Object>();
    item21.put("id", "About");
    item21.put("label", "About Adobe CVG Viewer...");
    Map<String, Object> item12 = new HashMap<String, Object>();
    item12.put("id", "FindAgain");
    item12.put("label", "Find Again");
    assertThat(json).contains(item12, Index.atIndex(12));
    Map<String, Object> item13 = new HashMap<String, Object>();
    item13.put("id", "Copy");
    assertThat(json).contains(item13, Index.atIndex(13));
  }

  static class Person {
    int age;
    String name;

    Person(int age, String name) {
      this.age = age;
      this.name = name;
    }
  }

  public void testObjectSerialization() throws Exception {
    Person person = new Person(21, "Prateek");
    Map<String, Object> sourceMap = new HashMap<String, Object>();
    sourceMap.put("person", person);
    sourceMap.put("extra", 40.32);
    String json = JsonUtils.fromMap(sourceMap);
    Map<String, Object> retrieved = JsonUtils.toMap(json);
    assertThat(retrieved).hasSize(2);
    assertThat(retrieved).contains(MapEntry.entry("person", person.toString()),
        MapEntry.entry("extra", 40.32));
  }

  public void testAllTypes() throws Exception {
    Random random = new Random();
    byte aByte = (byte) random.nextInt();
    short aShort = (short) random.nextInt();
    int anInt = random.nextInt();
    long aLong = random.nextLong();
    float aFloat = random.nextFloat();
    double aDouble = random.nextDouble();
    char aChar = (char) random.nextInt(256);
    String aString = UUID.randomUUID().toString(); // good enough
    boolean aBoolean = random.nextBoolean();

    Map<String, Object> sourceMap = new HashMap<String, Object>();
    sourceMap.put("aByte", aByte);
    sourceMap.put("aShort", aShort);
    sourceMap.put("anInt", anInt);
    sourceMap.put("aLong", aLong);
    sourceMap.put("aFloat", aFloat);
    sourceMap.put("aDouble", aDouble);
    sourceMap.put("aChar", aChar);
    sourceMap.put("aString", aString);
    sourceMap.put("aBoolean", aBoolean);

    String json = JsonUtils.fromMap(sourceMap);

    Map<String, Object> retrieved = JsonUtils.toMap(json);
    sourceMap.put("aFloat", (double) aFloat);

    assertThat(retrieved) //
        .contains(MapEntry.entry("aByte", (int) aByte))
        .contains(MapEntry.entry("aShort", (int) aShort))
        .contains(MapEntry.entry("anInt", anInt))
        .contains(MapEntry.entry("aFloat", Double.valueOf(String.valueOf(aFloat))))
        .contains(MapEntry.entry("aDouble", aDouble))
        .contains(MapEntry.entry("aChar", String.valueOf(aChar)))
        .contains(MapEntry.entry("aString", aString))
        .contains(MapEntry.entry("aBoolean", aBoolean));
  }
}
