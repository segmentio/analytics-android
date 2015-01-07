package com.segment.analytics.internal.model.payloads.util;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import java.util.LinkedHashMap;
import org.robolectric.Robolectric;

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
      traits = Traits.create(Robolectric.application);
    }
    if (context == null) {
      context = new AnalyticsContext(new LinkedHashMap<String, Object>());
      context.setTraits(traits);
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
