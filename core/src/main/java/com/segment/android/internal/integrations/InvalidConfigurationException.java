package com.segment.android.internal.integrations;

/**
 * Exception thrown when an integration could not be initialized due to an invalid configuration.
 * In most cases this would be due to missing permissions.
 */
public class InvalidConfigurationException extends Exception {
  public InvalidConfigurationException(String detailMessage) {
    super(detailMessage);
  }

  public InvalidConfigurationException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }
}