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
package com.segment.analytics;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

abstract class PayloadQueue implements Closeable {
    abstract int size();

    abstract void remove(int n) throws IOException;

    abstract void add(byte[] data) throws IOException;

    abstract void forEach(ElementVisitor visitor) throws IOException;

    interface ElementVisitor {
        /**
         * Called once per element.
         *
         * @param in stream of element data. Reads as many bytes as requested, unless fewer than the
         *     request number of bytes remains, in which case it reads all the remaining bytes. Not
         *     buffered.
         * @param length of element data in bytes
         * @return an indication whether the {@link #forEach} operation should continue; If {@code
         *     true}, continue, otherwise halt.
         */
        boolean read(InputStream in, int length) throws IOException;
    }

    static class PersistentQueue extends PayloadQueue {
        final QueueFile queueFile;

        PersistentQueue(QueueFile queueFile) {
            this.queueFile = queueFile;
        }

        @Override
        int size() {
            return queueFile.size();
        }

        @Override
        void remove(int n) throws IOException {
            try {
                queueFile.remove(n);
            } catch (ArrayIndexOutOfBoundsException e) {
                // Guard against ArrayIndexOutOfBoundsException, unfortunately root cause is
                // unknown.
                // Ref: https://github.com/segmentio/analytics-android/issues/449.
                throw new IOException(e);
            }
        }

        @Override
        void add(byte[] data) throws IOException {
            queueFile.add(data);
        }

        @Override
        void forEach(ElementVisitor visitor) throws IOException {
            queueFile.forEach(visitor);
        }

        @Override
        public void close() throws IOException {
            queueFile.close();
        }
    }

    static class MemoryQueue extends PayloadQueue {
        final LinkedList<byte[]> queue;

        MemoryQueue() {
            this.queue = new LinkedList<>();
        }

        @Override
        int size() {
            return queue.size();
        }

        @Override
        void remove(int n) throws IOException {
            for (int i = 0; i < n; i++) {
                queue.remove();
            }
        }

        @Override
        void add(byte[] data) throws IOException {
            queue.add(data);
        }

        @Override
        void forEach(ElementVisitor visitor) throws IOException {
            for (int i = 0; i < queue.size(); i++) {
                byte[] data = queue.get(i);
                boolean shouldContinue = visitor.read(new ByteArrayInputStream(data), data.length);
                if (!shouldContinue) {
                    return;
                }
            }
        }

        @Override
        public void close() throws IOException {
            // no-op
        }
    }
}
