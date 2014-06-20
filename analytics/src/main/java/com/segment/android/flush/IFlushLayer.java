package com.segment.android.flush;

import com.segment.android.models.Batch;
import com.segment.android.utils.IThreadedLayer;

/**
 * Handles flushing to the server endpoint
 */
public interface IFlushLayer extends IThreadedLayer {

  //
  // Callbacks
  //

  /**
   * Callback for the {@link com.segment.android.flush.IFlushLayer.flush} method
   */
  public interface FlushCallback {
    /**
     * Called when all messages have been flushed from the queue
     *
     * @param success True for successful flush, false for not.
     * @param batch The batch that was sent to the server
     */
    public void onFlushCompleted(boolean success, Batch batch);
  }

  //
  // Methods
  //

  /**
   * Triggers a flush from the local action database to the server.
   */
  public void flush(FlushCallback callback);
}
