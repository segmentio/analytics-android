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

package com.segment.android.internal.queue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.segment.android.internal.util.Logger;
import com.squareup.tape.FileObjectQueue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Use GSON to serialize classes to a bytes.
 * <p/>
 * This variant of {@link GsonConverter} works with anything you throw at it. It is however
 * important for Gson to be able to understand your inner complex objects/entities Use an {@link
 * InterfaceAdapter} for these purposes.
 */
public class GsonConverter<T> implements FileObjectQueue.Converter<T> {
  static final String CONCRETE_CLASS_NAME = "concrete_class_name";
  static final String CONCRETE_CLASS_OBJECT = "concrete_class_object";
  final Gson gson;

  public GsonConverter(Gson gson) {
    this.gson = gson;
  }

  @Override
  public T from(byte[] bytes) {
    Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
    JsonObject completeAbstractClassInfoAsJson = gson.fromJson(reader, JsonObject.class);

    Class<T> clazz;
    try {
      String className = completeAbstractClassInfoAsJson.get(CONCRETE_CLASS_NAME).getAsString();
      clazz = (Class<T>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      Logger.e(e, "Error while deserializing TapeTask to a concrete class");
      return null;
    }

    String objectDataAsString =
        completeAbstractClassInfoAsJson.get(CONCRETE_CLASS_OBJECT).getAsString();

    return gson.fromJson(objectDataAsString, clazz);
  }

  @Override
  public void toStream(T object, OutputStream bytes) throws IOException {
    Writer writer = new OutputStreamWriter(bytes);

    JsonObject completeAbstractClassInfoAsJson = new JsonObject();
    completeAbstractClassInfoAsJson.addProperty(CONCRETE_CLASS_NAME, object.getClass().getName());
    completeAbstractClassInfoAsJson.addProperty(CONCRETE_CLASS_OBJECT, gson.toJson(object));

    gson.toJson(completeAbstractClassInfoAsJson, writer);
    writer.close();
  }
}
