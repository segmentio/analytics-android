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

package com.segment.android.integration;

/**
 * Represents the life-cycle stages of a provider.
 */
public enum IntegrationState {

  /**
   * The initial starting state o the provider, when it hasn't been initialized with settings from
   * the server.
   */
  NOT_INITIALIZED(0),
  /**
   * The state at which settings have been provided, but they didn't have enough information to
   * initialize the provider.
   */
  INVALID(1),
  /**
   * The state at which settings were provided and the provider is ready to be enabled.
   */
  INITIALIZED(2),
  /**
   * The state at which the provider has been explicitly disabled by the server.
   */
  DISABLED(3),
  /**
   * The state at which the provider has been initialized, and enabled by the the library.
   */
  ENABLED(4),
  /**
   * The state at which the provider says that its ready to start processing data.
   */
  READY(5);

  private final int value;

  private IntegrationState(int value) {
    this.value = value;
  }

  public int getValue() {
    return this.value;
  }

  /**
   * Returns whether the current state is greater or equal to the other state provided.
   */
  public boolean ge(IntegrationState other) {
    return this.value >= other.value;
  }
}
