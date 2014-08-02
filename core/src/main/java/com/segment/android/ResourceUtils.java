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

import android.content.Context;
import android.content.res.Resources;

final class ResourceUtils {
  private ResourceUtils() {
    throw new AssertionError("No instances");
  }

  static String getString(Context context, String key) {
    int id = getIdentifier(context, "string", key);
    if (id != 0) {
      return context.getResources().getString(id);
    } else {
      return null;
    }
  }

  static boolean getBooleanOrThrow(Context context, String key) {
    int id = getIdentifier(context, "bool", key);
    if (id != 0) {
      return context.getResources().getBoolean(id);
    } else {
      // We won't ever have an error thrown since we check the id first, so we'll re-throw it up
      throw new Resources.NotFoundException("boolean with key:" + key + " not found in resources");
    }
  }

  static int getIntegerOrThrow(Context context, String key) {
    int id = getIdentifier(context, "integer", key);
    if (id != 0) {
      return context.getResources().getInteger(id);
    } else {
      // We won't ever have an error thrown since we check the id first, so we'll re-throw it up
      throw new Resources.NotFoundException("integer with key:" + key + " not found in resources");
    }
  }

  private static int getIdentifier(Context context, String type, String key) {
    return context.getResources().getIdentifier(key, type, context.getPackageName());
  }
}
