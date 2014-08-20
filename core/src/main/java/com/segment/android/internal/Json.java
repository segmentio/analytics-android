package com.segment.android.internal;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** A class to generate json from POJOs. */
@TargetApi(Build.VERSION_CODES.HONEYCOMB) class Json {
  public String toJson(Object object) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(bos));
    toJson(jsonWriter, object);
    jsonWriter.close();
    return new String(bos.toByteArray());
  }

  void toJson(JsonWriter jsonWriter, Object object) throws Exception {
    if (object == null) {
      jsonWriter.nullValue();
      return;
    }
    List<Field> fields = getAllFields(null, object.getClass());
    jsonWriter.beginObject();
    for (Field field : fields) {
      JsonType fieldType = getJsonType(field);
      switch (fieldType) {
        case INTEGER:
        case DOUBLE:
        case LONG:
          Number number = (Number) field.get(object);
          if (number != null) {
            jsonWriter.name(field.getName()).value(number);
          }
          break;
        case STRING:
          String string = (String) field.get(object);
          if (string != null) {
            jsonWriter.name(field.getName()).value(string);
          }
          break;
        case BOOLEAN:
          Boolean bool = (Boolean) field.get(object);
          if (bool != null) {
            jsonWriter.name(field.getName()).value(bool);
          }
          break;
        case COMPLEX:
          jsonWriter.name(field.getName());
          toJson(jsonWriter, field.get(object));
          break;
        default:
          throw new IllegalArgumentException("UnknownType!");
      }
    }
    jsonWriter.endObject();
  }

  List<Field> getAllFields(List<Field> fields, Class<?> type) {
    if (fields == null) fields = new ArrayList<Field>();
    for (Field field : type.getDeclaredFields()) {
      fields.add(field);
    }
    if (type.getSuperclass() != null) {
      fields = getAllFields(fields, type.getSuperclass());
    }
    return fields;
  }

  public <T> T fromJson(String json, Class<T> clazz) throws Exception {
    JsonReader reader =
        new JsonReader(new InputStreamReader(new ByteArrayInputStream(json.getBytes()), "UTF-8"));
    T object;
    try {
      object = read(reader, clazz);
    } finally {
      reader.close();
    }
    return object;
  }

  <T> T read(JsonReader reader, Class<T> clazz) throws Exception {
    Constructor<T> constructor = getConstructor(clazz);
    if (constructor == null) {
      return null;
    }
    T object = constructor.newInstance();
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      Field field = getField(object, name);
      if (field == null) {
        reader.skipValue();
        continue;
      }
      JsonType fieldType = getJsonType(field);
      switch (fieldType) {
        case INTEGER:
          field.setInt(object, reader.nextInt());
          break;
        case STRING:
          field.set(object, reader.nextString());
          break;
        case DOUBLE:
          field.set(object, reader.nextDouble());
          break;
        case LONG:
          field.set(object, reader.nextLong());
          break;
        case BOOLEAN:
          field.set(object, reader.nextBoolean());
          break;
        case COMPLEX:
          field.set(object, read(reader, field.getType()));
          break;
        default:
          reader.skipValue();
          break;
      }
    }
    reader.endObject();
    return object;
  }

  JsonType getJsonType(Field field) {
    return getJsonType(field.getType());
  }

  JsonType getJsonType(Class<?> clazz) {
    if (clazz == Integer.class
        || clazz == int.class
        || clazz == Short.class
        || clazz == short.class
        || clazz == Byte.class
        || clazz == byte.class) {
      return JsonType.INTEGER;
    }
    if (clazz == Long.class || clazz == long.class) {
      return JsonType.LONG;
    }
    if (clazz == Double.class
        || clazz == double.class
        || clazz == Float.class
        || clazz == float.class) {
      return JsonType.DOUBLE;
    }
    if (clazz == String.class || clazz == Character.class || clazz == char.class) {
      return JsonType.STRING;
    }
    if (clazz == boolean.class || clazz == Boolean.class) {
      return JsonType.BOOLEAN;
    }

    return JsonType.COMPLEX;
  }

  enum JsonType {
    INTEGER, LONG, DOUBLE, STRING, BOOLEAN, COMPLEX
    // todo: maps, collections, arrays
  }

  <T> Constructor<T> getConstructor(Class<T> c) {
    try {
      Constructor<T> declaredConstructor = c.getDeclaredConstructor();
      declaredConstructor.setAccessible(true);
      return declaredConstructor;
    } catch (Exception e) {
      return null;
    }
  }

  Field getField(Object target, String name) {
    try {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      return null;
    }
  }
}
