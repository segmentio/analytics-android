package com.segment.analytics;

import com.segment.analytics.core.tests.BuildConfig;
import java.io.IOException;
import okio.Buffer;
import okio.ByteString;
import okio.Okio;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class CryptoTest {
  @Test public void noneCryptoWrite() throws IOException {
    Crypto crypto = Crypto.none();
    ByteString foo = ByteString.encodeUtf8("foo");
    Buffer buffer = new Buffer();

    foo.write(crypto.encrypt(buffer.outputStream()));

    assertThat(buffer.readByteString()).isEqualTo(foo);
  }

  @Test public void noneCryptoRead() throws IOException {
    Crypto crypto = Crypto.none();
    ByteString foo = ByteString.encodeUtf8("foo");
    Buffer buffer = new Buffer();

    buffer.write(foo);

    assertThat(Okio.buffer(Okio.source(crypto.decrypt(buffer.inputStream()))).readByteString()) //
        .isEqualTo(foo);
  }
}
