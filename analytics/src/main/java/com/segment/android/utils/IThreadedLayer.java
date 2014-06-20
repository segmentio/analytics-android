package com.segment.android.utils;

/**
 * Controls the starting and stopped state of the threads backing
 * a specific application layer.
 */
public interface IThreadedLayer {

  /**
   * Starts the threads associated with this layer
   */
  void start();

  /**
   * Stops the threads associated with this layer
   */
  void quit();
}
