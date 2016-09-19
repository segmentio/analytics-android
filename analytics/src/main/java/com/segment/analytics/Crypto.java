package com.segment.analytics;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class Crypto {
  abstract InputStream decrypt(InputStream is);

  abstract OutputStream encrypt(OutputStream os);

  public static Crypto none() {
    return new Crypto() {
      @Override InputStream decrypt(InputStream is) {
        return is;
      }

      @Override OutputStream encrypt(OutputStream os) {
        return os;
      }
    };
  }
}
