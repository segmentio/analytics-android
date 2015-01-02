package com.segment.analytics;

import java.util.LinkedHashMap;
import org.robolectric.Robolectric;

public class IdentifyPayloadBuilder {
  private AnalyticsContext context;
  private Traits traits;
  private Options options;

  public IdentifyPayloadBuilder traits(Traits traits) {
    this.traits = traits;
    return this;
  }

  public IdentifyPayloadBuilder options(Options options) {
    this.options = options;
    return this;
  }

  public IdentifyPayloadBuilder context(AnalyticsContext context) {
    this.context = context;
    return this;
  }

  public IdentifyPayload build() {
    if (traits == null) {
      traits = Traits.create(Robolectric.application);
    }
    if (context == null) {
      context = new AnalyticsContext(new LinkedHashMap<String, Object>());
      context.setTraits(traits);
    }
    if (options == null) {
      options = new Options();
    }
    return new IdentifyPayload(context, options, traits);
  }
}
