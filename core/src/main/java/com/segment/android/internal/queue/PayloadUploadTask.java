package com.segment.android.internal.queue;

import com.segment.android.internal.SegmentHTTPApi;
import com.segment.android.internal.payload.BasePayload;
import com.squareup.tape.Task;
import java.io.IOException;

public class PayloadUploadTask implements Task<Void> {
  transient SegmentHTTPApi segmentHTTPApi;
  private final BasePayload payload;

  public PayloadUploadTask(BasePayload payload) {
    this.payload = payload;
  }

  public void setSegmentHTTPApi(SegmentHTTPApi segmentHTTPApi) {
    this.segmentHTTPApi = segmentHTTPApi;
  }

  public BasePayload payload() {
    return payload;
  }

  @Override public void execute(Void callback) {
    try {
      segmentHTTPApi.upload(payload);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
