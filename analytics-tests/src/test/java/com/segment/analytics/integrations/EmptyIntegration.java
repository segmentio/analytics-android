package com.segment.analytics.integrations;

import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;

@SuppressWarnings("unused")
public class EmptyIntegration extends AbstractIntegration<Void> {

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {

  }

  @Override public String key() {
    return "empty";
  }
}
