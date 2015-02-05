package com.segment.analytics.internal.model.payloads.util;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.internal.model.payloads.AliasPayload;

import static com.segment.analytics.TestUtils.createContext;
import static com.segment.analytics.TestUtils.createTraits;

public class AliasPayloadBuilder {
  private AnalyticsContext context;
  private Traits traits;
  private String previousId;
  private Options options;

  public AliasPayloadBuilder traits(Traits traits) {
    this.traits = traits;
    return this;
  }

  public AliasPayloadBuilder context(AnalyticsContext context) {
    this.context = context;
    return this;
  }

  public AliasPayloadBuilder previousId(String previousId) {
    this.previousId = previousId;
    return this;
  }

  public AliasPayloadBuilder options(Options options) {
    this.options = options;
    return this;
  }

  public AliasPayload build() {
    if (traits == null) {
      traits = createTraits();
    }
    if (context == null) {
      context = createContext(traits);
    }
    if (options == null) {
      options = new Options();
    }
    if (previousId == null) {
      previousId = "foo";
    }
    return new AliasPayload(context, options, previousId);
  }
}
