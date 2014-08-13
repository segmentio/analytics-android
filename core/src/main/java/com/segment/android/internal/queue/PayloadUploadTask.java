package com.segment.android.internal.queue;

import com.segment.android.internal.SegmentHTTPApi;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.util.Logger;
import com.squareup.tape.Task;
import java.io.IOException;

public class PayloadUploadTask implements Task<PayloadUploadTask.Callback> {
  transient SegmentHTTPApi segmentHTTPApi;
  private final BasePayload payload;

  public PayloadUploadTask(BasePayload payload) {
    this.payload = payload;
  }

  public interface Callback {
    void onSuccess();

    void onFailure();
  }

  public void setSegmentHTTPApi(SegmentHTTPApi segmentHTTPApi) {
    this.segmentHTTPApi = segmentHTTPApi;
  }

  public BasePayload payload() {
    return payload;
  }

  @Override public void execute(Callback callback) {
    try {
      segmentHTTPApi.upload(payload);
      Logger.v("Uploaded payload");
      callback.onSuccess();
    } catch (IOException e) {
      Logger.e(e, "Failed to Upload");
      callback.onFailure();
    }
  }
}
