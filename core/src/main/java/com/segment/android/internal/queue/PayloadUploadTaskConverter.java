package com.segment.android.internal.queue;

import com.segment.android.internal.payload.BasePayload;
import com.squareup.tape.FileObjectQueue;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class PayloadUploadTaskConverter implements FileObjectQueue.Converter<PayloadUploadTask> {
  @Override public PayloadUploadTask from(byte[] bytes) throws IOException {
    String string = new String(bytes);
    BasePayload payload = new BasePayload(string);
    return new PayloadUploadTask(payload);
  }

  @Override public void toStream(PayloadUploadTask o, OutputStream bytes) throws IOException {
    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(bytes);
    outputStreamWriter.write(o.payload().toString());
    outputStreamWriter.close();
  }
}
