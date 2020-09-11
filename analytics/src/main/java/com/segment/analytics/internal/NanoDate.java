/**
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
package com.segment.analytics.internal;

import java.util.Date;

public class NanoDate extends Date {
    /*
     * Java Genetic Algorithm Library (@__identifier__@).
     * Copyright (c) @__year__@ Franz Wilhelmstötter
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     *
     * Author:
     *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
     */
    public static final class NanoClock {
        /**
         * Jenetics' NanoClock to get higher resolution timestamps, pruned to our needs. Forked from
         * this file
         * https://github.com/jenetics/jenetics/blob/master/jenetics/src/main/java/io/jenetics/util/NanoClock.java
         */
        private static final long EPOCH_NANOS = System.currentTimeMillis() * 1_000_000;

        private static final long NANO_START = System.nanoTime();
        private static final long OFFSET_NANOS = EPOCH_NANOS - NANO_START;

        /**
         * This returns the nanosecond-based instant, measured from 1970-01-01T00:00Z (UTC). This
         * method will return valid values till the year 2262.
         *
         * @return the nanosecond-based instant, measured from 1970-01-01T00:00Z (UTC)
         */
        private long nanos() {
            return System.nanoTime() + OFFSET_NANOS;
        }

        public static long currentTimeNanos() {
            return new NanoClock().nanos();
        }
    }

    private long nanos;

    public NanoDate() {
        this(NanoClock.currentTimeNanos());
    }

    public NanoDate(Date d) {
        this(d.getTime() * 1_000_000);
    }

    public NanoDate(long nanos) {
        super(nanos / 1_000_000);
        this.nanos = nanos;
    }

    public long nanos() {
        return nanos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NanoDate) {
            return ((NanoDate) obj).nanos() == nanos();
        } else if (obj instanceof Date) {
            return super.equals(obj) && nanos % 1_000_000 == 0;
        }
        return false;
    }
}
