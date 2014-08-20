package com.segment.android.internal;

import com.segment.android.BaseAndroidTestCase;

import static org.fest.assertions.api.Assertions.assertThat;

public class JsonTest extends BaseAndroidTestCase {
  static class Message {
    long id;
    String text;
    double size;
    Boolean read; // Test boxed primitive
    Info info;
  }

  static class Info {
    String thread;
    int id;
  }

  static class Message1 {
    long id;
    String text;
    double size;
    Boolean read; // Test boxed primitive
    Info info;

    Message1(long id, String text, double size, Boolean read) {
      this.id = id;
      this.text = text;
      this.size = size;
      this.read = read;
    }
  }

  Json generator;

  @Override public void setUp() throws Exception {
    super.setUp();
    generator = new Json();
  }

  public void testBoxedTypes() throws Exception {
    assertThat(generator.getJsonType(Integer.class)).isEqualTo(Json.JsonType.INTEGER);
    assertThat(generator.getJsonType(Short.class)).isEqualTo(Json.JsonType.INTEGER);
    assertThat(generator.getJsonType(Byte.class)).isEqualTo(Json.JsonType.INTEGER);
    assertThat(generator.getJsonType(Long.class)).isEqualTo(Json.JsonType.LONG);
    assertThat(generator.getJsonType(Double.class)).isEqualTo(Json.JsonType.DOUBLE);
    assertThat(generator.getJsonType(Float.class)).isEqualTo(Json.JsonType.DOUBLE);
    assertThat(generator.getJsonType(String.class)).isEqualTo(Json.JsonType.STRING);
    assertThat(generator.getJsonType(Character.class)).isEqualTo(Json.JsonType.STRING);
    assertThat(generator.getJsonType(Boolean.class)).isEqualTo(Json.JsonType.BOOLEAN);
  }

  public void testComplexTypes() throws Exception {
    assertThat(generator.getJsonType(Message.class)).isEqualTo(Json.JsonType.COMPLEX);

    // TODO : tests for Maps, Collections, Arrays
  }

  public void testPrimitiveTypes() throws Exception {
    assertThat(generator.getJsonType(int.class)).isEqualTo(Json.JsonType.INTEGER);
    assertThat(generator.getJsonType(short.class)).isEqualTo(Json.JsonType.INTEGER);
    assertThat(generator.getJsonType(byte.class)).isEqualTo(Json.JsonType.INTEGER);
    assertThat(generator.getJsonType(long.class)).isEqualTo(Json.JsonType.LONG);
    assertThat(generator.getJsonType(double.class)).isEqualTo(Json.JsonType.DOUBLE);
    assertThat(generator.getJsonType(float.class)).isEqualTo(Json.JsonType.DOUBLE);
    assertThat(generator.getJsonType(char.class)).isEqualTo(Json.JsonType.STRING);
    assertThat(generator.getJsonType(boolean.class)).isEqualTo(Json.JsonType.BOOLEAN);
  }

  public void testMessageWithNulls() throws Exception {
    String json =
        "{\"id\":912345678902,\"text\":\"@android_newb just use android.util.JsonReader!\"}";

    Message message = generator.fromJson(json, Message.class);
    assertThat(message.id).isEqualTo(912345678902L);
    assertThat(message.text).isEqualTo("@android_newb just use android.util.JsonReader!");
    assertThat(message.size).isEqualTo(0.0d);
    assertThat(message.read).isEqualTo(null);
    assertThat(message.info).isNull();
  }

  public void testMessageWithPrimitives() throws Exception {
    String json =
        "{\"size\":10.45,\"read\":false,\"id\":912345678902,\"text\":\"@android_newb just use android.util.JsonReader!\"}";

    Message message = generator.fromJson(json, Message.class);
    assertThat(message.id).isEqualTo(912345678902L);
    assertThat(message.text).isEqualTo("@android_newb just use android.util.JsonReader!");
    assertThat(message.size).isEqualTo(10.45);
    assertThat(message.read).isEqualTo(false);
    assertThat(message.info).isNull();
  }

  public void testFullMessage() throws Exception {
    String json =
        "{\"size\":10.45,\"read\":false,\"id\":912345678902,\"text\":\"@android_newb just use android.util.JsonReader!\",\"info\":{\"thread\":\"main\",\"id\":2131}}";

    Message message = generator.fromJson(json, Message.class);
    assertThat(message.id).isEqualTo(912345678902L);
    assertThat(message.text).isEqualTo("@android_newb just use android.util.JsonReader!");
    assertThat(message.size).isEqualTo(10.45);
    assertThat(message.read).isEqualTo(false);
    assertThat(message.info).isNotNull();
    assertThat(message.info.id).isEqualTo(2131);
    assertThat(message.info.thread).isEqualTo("main");
  }

  public void testMessageWithNoConstructor() throws Exception {
    String json =
        "{\"size\":10.45,\"read\":false,\"id\":912345678902,\"text\":\"@android_newb just use android.util.JsonReader!\"}";
    assertThat(generator.fromJson(json, Message1.class)).isNull();
  }

  static class Nested1 {
    String id;
    long timestamp;
  }

  static class Nested2 extends Nested1 {
    boolean read;
  }

  static class Nested3 extends Nested2 {
    Message message;
    double length;
  }

  public void testRetrievesAllFields() throws Exception {
    assertThat(generator.getAllFields(null, Message.class)).isNotNull().hasSize(5);
    assertThat(generator.getAllFields(null, Message1.class)).isNotNull().hasSize(5);
    assertThat(generator.getAllFields(null, Info.class)).isNotNull().hasSize(2);
    assertThat(generator.getAllFields(null, Nested1.class)).isNotNull().hasSize(2);
    assertThat(generator.getAllFields(null, Nested2.class)).isNotNull().hasSize(3);
    assertThat(generator.getAllFields(null, Nested3.class)).isNotNull().hasSize(5);
  }

  public void testToJson() throws Exception {
    Message message = new Message();
    message.id = 912345678902L;
    message.text = "@android_newb just use android.util.JsonReader!";
    assertThat(generator.toJson(message)).contains("\"id\":912345678902")
        .contains("\"text\":\"@android_newb just use android.util.JsonReader!\"");
    message.read = false;
    assertThat(generator.toJson(message)).contains("\"id\":912345678902")
        .contains("\"text\":\"@android_newb just use android.util.JsonReader!\"")
        .contains("\"read\":false");
  }
}