package com.segment.analytics.test;

import static com.segment.analytics.Utils.createContext;
import static com.segment.analytics.Utils.createTraits;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.IdentifyPayload;

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
      traits = createTraits();
    }
    if (context == null) {
      context = createContext(traits);
    }
    if (options == null) {
      options = new Options();
    }

    return new IdentifyPayload.Builder()
        .userId(traits.userId())
        .traits(traits)
        .anonymousId(traits.anonymousId())
        .context(context)
        .integrations(options.integrations())
        .build();
  }
}
