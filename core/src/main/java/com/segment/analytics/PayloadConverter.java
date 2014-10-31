package com.segment.analytics;

import com.squareup.tape.FileObjectQueue;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static com.segment.analytics.Utils.isNullOrEmpty;

class PayloadConverter implements FileObjectQueue.Converter<BasePayload> {
  @Override public BasePayload from(byte[] bytes) throws IOException {
    String json = new String(bytes);
    if (isNullOrEmpty(json)) {
      throw new IOException("Cannot serialize payload from empty byte array.");
    }
    return new BasePayload(json);
  }

  @Override public void toStream(BasePayload payload, OutputStream bytes) throws IOException {
    String json = payload.toString();
    if (isNullOrEmpty(json)) {
      throw new IOException("Cannot deserialize payload : " + payload);
    }
    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(bytes);
    outputStreamWriter.write(json);
    outputStreamWriter.close();
  }
}
