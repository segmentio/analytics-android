package com.segment.analytics

import okio.Buffer
import okio.ByteString
import okio.Okio
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CryptoTest {

  @Test
  @Throws(IOException::class)
  fun noneCryptoWrite() {
    val crypto: Crypto = Crypto.none()
    val foo: ByteString = ByteString.encodeUtf8("foo")
    val buffer = Buffer()

    foo.write(crypto.encrypt(buffer.outputStream()))

    Assertions.assertThat(buffer.readByteString()).isEqualTo(foo)
  }

  @Test
  fun cryptoRead() {
    val crypto: Crypto = Crypto.none()
    val foo: ByteString = ByteString.encodeUtf8("foo")
    val buffer = Buffer()

    buffer.write(foo)

    Assertions.assertThat(Okio.buffer(Okio.source(crypto.decrypt(buffer.inputStream()))).readByteString())
        .isEqualTo(foo)
  }
}