package com.segment.analytics;

/**
 * Exception thrown when an integration could not be initialized due to an invalid configuration.
 * In most cases this would be due to missing permissions.
 */
class InvalidConfigurationException extends Exception {
  InvalidConfigurationException(String detailMessage) {
    super(detailMessage);
  }
}