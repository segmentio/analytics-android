package com.segment.analytics.wear.model;

import com.segment.analytics.Properties;
import com.segment.analytics.json.JsonMap;

public class WearScreenPayload extends JsonMap {
  private static final String CATEGORY_KEY = "category";
  private static final String NAME_KEY = "name";
  private static final String PROPERTIES_KEY = "properties";

  public WearScreenPayload(String json) {
    super(json);
  }

  public WearScreenPayload(String category, String name, Properties properties) {
    put(CATEGORY_KEY, category);
    put(NAME_KEY, name);
    put(PROPERTIES_KEY, properties);
  }

  public Properties getProperties() {
    return getJsonMap(PROPERTIES_KEY, Properties.class);
  }

  public String getCategory() {
    return getString(CATEGORY_KEY);
  }

  public String getName() {
    return getString(NAME_KEY);
  }
}
