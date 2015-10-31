package com.segment.analytics.integrations;

import android.content.Context;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;

@SuppressWarnings("unused")
public class InvalidIntegration extends AbstractIntegration<Void> {

  // Integrations MUST have a no-args constructor.
  public InvalidIntegration(final String foo) {
    throw new AssertionError("Must not be invoked by IntegrationManager.");
  }

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {

  }

  @Override public String key() {
    return "empty";
  }
}
