package com.segment.android.internal.queue;

import com.segment.android.internal.payload.BasePayload;
import com.squareup.tape.FileObjectQueue;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class PayloadConverter implements FileObjectQueue.Converter<BasePayload> {
  @Override public BasePayload from(byte[] bytes) throws IOException {
    String string = new String(bytes);
    return new BasePayload(string);
  }

  @Override public void toStream(BasePayload o, OutputStream bytes) throws IOException {
    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(bytes);
    outputStreamWriter.write(o.toString());
    outputStreamWriter.close();
  }
}
