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

// @formatter:off
// @formatter:on

package com.localytics.android;

import java.lang.reflect.InvocationTargetException;

/**
 * Static utilities for performing reflection against newer Android SDKs.
 * <p>
 * This is not a general-purpose reflection class but is rather specifically designed for calling
 * methods that must exist in newer
 * versions of Android.
 */
public final class ReflectionUtils {
  /**
   * Private constructor prevents instantiation
   *
   * @throws UnsupportedOperationException because this class cannot be instantiated.
   */
  private ReflectionUtils() {
    throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
  }

  /**
   * Use reflection to invoke a static method for a class object and method name
   *
   * @param <T> Type that the method should return
   * @param classObject Class on which to invoke {@code methodName}. Cannot be null.
   * @param methodName Name of the method to invoke. Cannot be null.
   * @param types explicit types for the objects. This is useful if the types are primitives, rather
   * than objects.
   * @param args arguments for the method. May be null if the method takes no arguments.
   * @return The result of invoking the named method on the given class for the args
   * @throws RuntimeException if the class or method doesn't exist
   */
  @SuppressWarnings("unchecked")
  public static <T> T tryInvokeStatic(final Class<?> classObject, final String methodName,
      final Class<?>[] types, final Object[] args) {
    return (T) helper(null, classObject, null, methodName, types, args);
  }

  /**
   * Use reflection to invoke a static method for a class object and method name
   *
   * @param <T> Type that the method should return
   * @param className Name of the class on which to invoke {@code methodName}. Cannot be null.
   * @param methodName Name of the method to invoke. Cannot be null.
   * @param types explicit types for the objects. This is useful if the types are primitives, rather
   * than objects.
   * @param args arguments for the method. May be null if the method takes no arguments.
   * @return The result of invoking the named method on the given class for the args
   * @throws RuntimeException if the class or method doesn't exist
   */
  @SuppressWarnings("unchecked")
  public static <T> T tryInvokeStatic(final String className, final String methodName,
      final Class<?>[] types, final Object[] args) {
    return (T) helper(className, null, null, methodName, types, args);
  }

  /**
   * Use reflection to invoke a static method for a class object and method name
   *
   * @param <T> Type that the method should return
   * @param target Object instance on which to invoke {@code methodName}. Cannot be null.
   * @param methodName Name of the method to invoke. Cannot be null.
   * @param types explicit types for the objects. This is useful if the types are primitives, rather
   * than objects.
   * @param args arguments for the method. May be null if the method takes no arguments.
   * @return The result of invoking the named method on the given class for the args
   * @throws RuntimeException if the class or method doesn't exist
   */
  @SuppressWarnings("unchecked")
  public static <T> T tryInvokeInstance(final Object target, final String methodName,
      final Class<?>[] types, final Object[] args) {
    return (T) helper(target, null, null, methodName, types, args);
  }

  private static <T> T helper(final Object target, final Class<?> classObject,
      final String className, final String methodName, final Class<?>[] argTypes,
      final Object[] args) {
    try {
      Class<?> cls;
      if (classObject != null) {
        cls = classObject;
      } else if (target != null) {
        cls = target.getClass();
      } else {
        cls = Class.forName(className);
      }

      return (T) cls.getMethod(methodName, argTypes).invoke(target, args);
    } catch (final NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (final InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}