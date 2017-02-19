package com.segment.analytics.test;

import static com.segment.analytics.Utils.createContext;
import static com.segment.analytics.Utils.createTraits;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.integrations.AliasPayload;

public class AliasPayloadBuilder {

  private AnalyticsContext context;
  private Traits traits;
  private String newId;
  private Options options;

  public AliasPayloadBuilder traits(Traits traits) {
    this.traits = traits;
    return this;
  }

  public AliasPayloadBuilder context(AnalyticsContext context) {
    this.context = context;
    return this;
  }

  public AliasPayloadBuilder newId(String newId) {
    this.newId = newId;
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
    if (newId == null) {
      newId = "foo";
    }
    return new AliasPayload(context, options, newId);
  }
}
