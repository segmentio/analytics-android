package com.segment.analytics.test;

import static com.segment.analytics.Utils.createContext;
import static com.segment.analytics.Utils.createTraits;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.integrations.ScreenPayload;

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
      traits = createTraits();
    }
    if (context == null) {
      context = createContext(traits);
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

    return new ScreenPayload.Builder()
        .category(category)
        .name(name)
        .properties(properties)
        .anonymousId(traits.anonymousId())
        .context(context)
        .integrations(options.integrations())
        .build();
  }
}
