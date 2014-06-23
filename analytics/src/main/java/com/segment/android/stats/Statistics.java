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

package com.segment.android.stats;

import java.util.concurrent.ConcurrentHashMap;

public class Statistics extends ConcurrentHashMap<String, Statistic> {

  private static final long serialVersionUID = -8837006750327885446L;

  public Statistic ensure(String key) {
    if (this.containsKey(key)) return this.get(key);

    Statistic statistic = new Statistic();
    this.put(key, statistic);
    return statistic;
  }

  public void update(String operation, double val) {
    if (!this.containsKey(operation)) {
      this.putIfAbsent(operation, new Statistic());
    }

    this.get(operation).update(val);
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();

    builder.append("\n-------- Safe Client Statistics --------\n");

    for (Entry<String, Statistic> entry : entrySet()) {

      String operation = entry.getKey();
      Statistic statistic = entry.getValue();

      builder.append(String.format("%s : %s\n", operation, statistic.toString()));
    }

    builder.append("----------------------------------------\n");

    return builder.toString();
  }
}