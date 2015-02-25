package com.segment.analytics.integrations;

import android.content.Context;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;

public class EmptyIntegration extends AbstractIntegration<Void> {
  @Override public void initialize(Context context, ValueMap settings, Analytics.LogLevel logLevel)
      throws IllegalStateException {

  }

  @Override public String key() {
    return "empty";
  }
}
