package com.segment.analytics.internal.model.payloads.util;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import org.robolectric.Robolectric;

import static com.segment.analytics.TestUtils.createContext;

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
      context = createContext(traits);
    }
    if (options == null) {
      options = new Options();
    }
    return new IdentifyPayload(context, options, traits);
  }
}
