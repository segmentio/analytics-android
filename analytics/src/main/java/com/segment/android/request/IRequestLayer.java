package com.segment.android.request;

import com.segment.android.models.Batch;
import com.segment.android.utils.IThreadedLayer;

/**
 * Handles sending requests to the server end point
 */
public interface IRequestLayer extends IThreadedLayer {

  //
  // Callbacks
  //

  /**
   * Callback for the #flush method
   */
  public interface RequestCallback {
    /**
     * Called when a request to the server is completed.
     *
     * @param success True for a successful request, false for not.
     */
    public void onRequestCompleted(boolean success);
  }

  //
  // Methods
  //

  /**
   * Send an action batch to the server.
   */
  public void send(Batch batch, RequestCallback callback);
}
