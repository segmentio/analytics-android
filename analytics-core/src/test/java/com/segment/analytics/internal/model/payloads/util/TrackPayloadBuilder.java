package com.segment.analytics.internal.model.payloads.util;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.LinkedHashMap;
import org.robolectric.Robolectric;

public class TrackPayloadBuilder {
  private AnalyticsContext context;
  private Traits traits;
  private String event;
  private Properties properties;
  private Options options;

  public TrackPayloadBuilder context(AnalyticsContext context) {
    this.context = context;
    return this;
  }

  public TrackPayloadBuilder traits(Traits traits) {
    this.traits = traits;
    return this;
  }

  public TrackPayloadBuilder event(String event) {
    this.event = event;
    return this;
  }

  public TrackPayloadBuilder properties(Properties properties) {
    this.properties = properties;
    return this;
  }

  public TrackPayloadBuilder options(Options options) {
    this.options = options;
    return this;
  }

  public TrackPayload build() {
    if (traits == null) {
      traits = Traits.create(Robolectric.application);
    }
    if (event == null) {
      event = "bar";
    }
    if (context == null) {
      context = new AnalyticsContext(new LinkedHashMap<String, Object>());
      context.setTraits(traits);
    }
    if (properties == null) {
      properties = new Properties();
    }
    if (options == null) {
      options = new Options();
    }
    return new TrackPayload(context, options, event, properties);
  }
}
