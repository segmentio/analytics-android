package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import okio.Buffer;
import okio.ByteString;
import okio.Okio;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class CryptoTest {
  @Test
  public void noneCryptoWrite() throws IOException {
    Crypto crypto = Crypto.none();
    ByteString foo = ByteString.encodeUtf8("foo");
    Buffer buffer = new Buffer();

    foo.write(crypto.encrypt(buffer.outputStream()));

    assertThat(buffer.readByteString()).isEqualTo(foo);
  }

  @Test
  public void noneCryptoRead() throws IOException {
    Crypto crypto = Crypto.none();
    ByteString foo = ByteString.encodeUtf8("foo");
    Buffer buffer = new Buffer();

    buffer.write(foo);

    assertThat(Okio.buffer(Okio.source(crypto.decrypt(buffer.inputStream()))).readByteString()) //
        .isEqualTo(foo);
  }
}
