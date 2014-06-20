package com.segment.android.info;

/**
 * A module that knows how to get a peice of information about
 * the phone
 */
public interface Info<T> {

  /**
   * Fetches the context key for this piece of info
   */
  public String getKey();

  /**
   * Returns a primitive object or a {@link org.json.JSONObject} that
   * represents this piece of information
   *
   * @param context The Android Application Context
   */
  public T get(android.content.Context context);
}
