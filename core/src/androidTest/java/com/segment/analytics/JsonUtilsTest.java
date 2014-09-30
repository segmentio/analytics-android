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

package com.segment.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.assertj.core.data.Index;
import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class JsonUtilsTest {
  @Test public void simpleJsonToMap() throws Exception {
    Map<String, Object> json = JsonUtils.jsonToMap(TestUtils.SAMPLE_JSON);

    assertThat(json).hasSize(1);
    assertThat(json).containsKey("glossary");

    Map<String, Object> glossary = (Map<String, Object>) json.get("glossary");
    assertThat(glossary).hasSize(2);
    assertThat(glossary).containsKey("title");
    assertThat((String) glossary.get("title")).isEqualTo("example glossary");
    assertThat(glossary).containsKey("GlossDiv");

    Map<String, Object> glossDiv = (Map<String, Object>) glossary.get("GlossDiv");
    assertThat(glossDiv).hasSize(2);
    assertThat(glossDiv).containsKey("title");
    assertThat((String) glossDiv.get("title")).isEqualTo("S");
    assertThat(glossDiv).containsKey("GlossList");

    Map<String, Object> glossList = (Map<String, Object>) glossDiv.get("GlossList");
    assertThat(glossList).hasSize(1);
    assertThat(glossList).containsKey("GlossEntry");

    Map<String, Object> glossEntry = (Map<String, Object>) glossList.get("GlossEntry");
    assertThat(glossEntry).hasSize(7);
    assertThat(glossEntry).containsKey("ID")
        .containsKey("SortAs")
        .containsKey("GlossTerm")
        .containsKey("Acronym")
        .containsKey("Abbrev")
        .containsKey("GlossDef")
        .containsKey("GlossSee");

    Map<String, Object> glossDef = (Map<String, Object>) glossEntry.get("GlossDef");
    assertThat(glossDef).hasSize(2);
    assertThat(glossDef).containsKey("para").containsKey("GlossSeeAlso");

    List<Object> glossSeeAlso = (List<Object>) glossDef.get("GlossSeeAlso");
    assertThat(glossSeeAlso).hasSize(2).contains("GML", "XML");
  }

  @Test public void array() throws Exception {
    List<Object> json = JsonUtils.jsonToList(TestUtils.SAMPLE_JSON_LIST);

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

  @Test public void primitiveArray() throws Exception {
    List<Object> json = JsonUtils.jsonToList("[0,375,668,5,6]");

    assertThat(json).hasSize(5);
    assertThat(json) //
        .contains(0.0, Index.atIndex(0))
        .contains(375.0, Index.atIndex(1))
        .contains(668.0, Index.atIndex(2))
        .contains(5.0, Index.atIndex(3))
        .contains(6.0, Index.atIndex(4));
  }

  @Test public void objectSerialization() throws Exception {
    Person person = new Person(21, "Prateek");
    Map<String, Object> sourceMap = new HashMap<String, Object>();
    sourceMap.put("person", person);
    sourceMap.put("extra", 40.32);
    String json = JsonUtils.mapToJson(sourceMap);
    Map<String, Object> retrieved = JsonUtils.jsonToMap(json);
    assertThat(retrieved).hasSize(2);
    assertThat(retrieved).contains(MapEntry.entry("person", person.toString()),
        MapEntry.entry("extra", 40.32));
  }

  @SuppressWarnings("UnnecessaryBoxing") @Test public void allTypes() throws Exception {
    Random random = new Random();
    byte aByte = (byte) random.nextInt();
    Byte aBoxedByte = Byte.valueOf((byte) random.nextInt());
    short aShort = (short) random.nextInt();
    Short aBoxedShort = Short.valueOf((short) random.nextInt());
    int anInt = random.nextInt();
    Integer aBoxedInt = Integer.valueOf(random.nextInt());
    long aLong = random.nextLong();
    Long aBoxedLong = Long.valueOf(random.nextLong());
    float aFloat = random.nextFloat();
    Float aBoxedFloat = Float.valueOf(random.nextFloat());
    double aDouble = random.nextDouble();
    Double aBoxedDouble = Double.valueOf(random.nextDouble());
    char aChar = (char) random.nextInt(256);
    Character aBoxedChar = Character.valueOf((char) random.nextInt(256));
    String aString = UUID.randomUUID().toString();
    boolean aBoolean = random.nextBoolean();
    Boolean aBoxedBoolean = Boolean.valueOf(random.nextBoolean());

    Map<String, Object> sourceMap = new HashMap<String, Object>();
    sourceMap.put("aByte", aByte);
    sourceMap.put("aBoxedByte", aBoxedByte);
    sourceMap.put("aShort", aShort);
    sourceMap.put("aBoxedShort", aBoxedShort);
    sourceMap.put("anInt", anInt);
    sourceMap.put("aBoxedInt", aBoxedInt);
    sourceMap.put("aLong", aLong);
    sourceMap.put("aBoxedLong", aBoxedLong);
    sourceMap.put("aFloat", aFloat);
    sourceMap.put("aBoxedFloat", aBoxedFloat);
    sourceMap.put("aDouble", aDouble);
    sourceMap.put("aBoxedDouble", aBoxedDouble);
    sourceMap.put("aChar", aChar);
    sourceMap.put("aBoxedChar", aBoxedChar);
    sourceMap.put("aString", aString);
    sourceMap.put("aBoolean", aBoolean);
    sourceMap.put("aBoxedBoolean", aBoxedBoolean);

    String json = JsonUtils.mapToJson(sourceMap);
    Map<String, Object> retrieved = JsonUtils.jsonToMap(json);
    assertThat(retrieved) //
        .contains(MapEntry.entry("aByte", Double.valueOf(aByte)))
        .contains(MapEntry.entry("aBoxedByte", Double.valueOf(aBoxedByte)))
        .contains(MapEntry.entry("aShort", Double.valueOf(aShort)))
        .contains(MapEntry.entry("aBoxedShort", Double.valueOf(aBoxedShort)))
        .contains(MapEntry.entry("anInt", Double.valueOf(anInt)))
        .contains(MapEntry.entry("aBoxedInt", Double.valueOf(aBoxedInt)))
        .contains(MapEntry.entry("aFloat", Double.valueOf(String.valueOf(aFloat))))
        .contains(MapEntry.entry("aBoxedFloat", Double.valueOf(String.valueOf(aBoxedFloat))))
        .contains(MapEntry.entry("aDouble", aDouble))
        .contains(MapEntry.entry("aBoxedDouble", aBoxedDouble))
        .contains(MapEntry.entry("aChar", String.valueOf(aChar)))
        .contains(MapEntry.entry("aBoxedChar", String.valueOf(aBoxedChar)))
        .contains(MapEntry.entry("aString", aString))
        .contains(MapEntry.entry("aBoolean", aBoolean))
        .contains(MapEntry.entry("aBoxedBoolean", aBoxedBoolean));
  }

  static class Person {
    int age;
    String name;

    Person(int age, String name) {
      this.age = age;
      this.name = name;
    }
  }
}
