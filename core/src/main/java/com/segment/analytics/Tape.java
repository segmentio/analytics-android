package com.segment.analytics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class Tape<E> extends AbstractQueue<E> {
  /** Backing storage implementation. */
  private final QueueFile queueFile;
  /** Keep file around for error reporting. */
  private final File file;
  /** Reusable byte output buffer. */
  private final DirectByteArrayOutputStream bytes = new DirectByteArrayOutputStream();
  /** Arbitrer to convert from bytes to concrete types and vice versa. */
  private final Converter<E> converter;

  Tape(File file, Converter<E> converter) throws IOException {
    this.file = file;
    this.queueFile = new QueueFile(file);
    this.converter = converter;
  }

  @Override public Iterator<E> iterator() {
    final List<E> elements = new ArrayList<E>();
    try {
      queueFile.forEach(new QueueFile.ElementReader() {
        @Override
        public void read(InputStream in, int length) throws IOException {
          byte[] data = new byte[length];
          in.read(data, 0, length);
          elements.add(converter.from(data));
        }
      });
    } catch (IOException e) {
      throw new FileException("Unable to iterate over QueueFile contents.", e, file);
    }
    return elements.iterator();
  }

  @Override public int size() {
    return queueFile.size();
  }

  @Override public boolean offer(E e) {
    if (e == null) {
      throw new IllegalArgumentException("null element may not be inserted.");
    }
    try {
      bytes.reset();
      converter.toStream(e, bytes);
      queueFile.add(bytes.getArray(), 0, bytes.size());
    } catch (IOException exception) {
      throw new FileException("Failed to add entry.", exception, file);
    }
    return true;
  }

  @Override public E poll() {
    E e = peek();
    if (e == null) {
      return null;
    } else {
      try {
        queueFile.remove();
        return e;
      } catch (IOException exception) {
        throw new FileException("Failed to poll.", exception, file);
      }
    }
  }

  @Override public E peek() {
    try {
      byte[] bytes = queueFile.peek();
      if (bytes == null) return null;
      return converter.from(bytes);
    } catch (IOException e) {
      throw new FileException("Failed to peek.", e, file);
    }
  }

  /**
   * Convert a byte stream to and from a concrete type.
   *
   * @param <T> Object type.
   */
  interface Converter<T> {
    /** Converts bytes to an object. */
    T from(byte[] bytes) throws IOException;

    /** Converts o to bytes written to the specified stream. */
    void toStream(T o, OutputStream bytes) throws IOException;
  }

  /** Enables direct access to the internal array. Avoids unnecessary copying. */
  private static class DirectByteArrayOutputStream extends ByteArrayOutputStream {
    DirectByteArrayOutputStream() {
      super();
    }

    /**
     * Gets a reference to the internal byte array.  The {@link #size()} method indicates how many
     * bytes contain actual data added since the last {@link #reset()} call.
     */
    byte[] getArray() {
      return buf;
    }
  }

  /** Encapsulates an {@link IOException} in an extension of {@link RuntimeException}. */
  static class FileException extends RuntimeException {
    private final File file;

    FileException(String message, IOException e, File file) {
      super(message, e);
      this.file = file;
    }

    File getFile() {
      return file;
    }
  }
}
