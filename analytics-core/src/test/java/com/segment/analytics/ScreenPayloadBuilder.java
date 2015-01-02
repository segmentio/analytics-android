package com.segment.analytics;

import java.util.LinkedHashMap;
import org.robolectric.Robolectric;

public class ScreenPayloadBuilder {
  private AnalyticsContext context;
  private Traits traits;
  private String category;
  private String name;
  private Properties properties;
  private Options options;

  public ScreenPayloadBuilder context(AnalyticsContext context) {
    this.context = context;
    return this;
  }

  public ScreenPayloadBuilder traits(Traits traits) {
    this.traits = traits;
    return this;
  }

  public ScreenPayloadBuilder category(String category) {
    this.category = category;
    return this;
  }

  public ScreenPayloadBuilder name(String name) {
    this.name = name;
    return this;
  }

  public ScreenPayloadBuilder properties(Properties properties) {
    this.properties = properties;
    return this;
  }

  public ScreenPayloadBuilder options(Options options) {
    this.options = options;
    return this;
  }

  public ScreenPayload build() {
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
    if (category == null && name == null) {
      category = "foo";
      name = "bar";
    }
    if (properties == null) {
      properties = new Properties();
    }
    return new ScreenPayload(context, options, category, name, properties);
  }
}

