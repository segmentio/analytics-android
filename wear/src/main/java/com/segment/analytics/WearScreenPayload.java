package com.segment.analytics;

import java.util.Map;

class WearScreenPayload extends ValueMap {
  private static final String CATEGORY_KEY = "category";
  private static final String NAME_KEY = "name";
  private static final String PROPERTIES_KEY = "properties";

  // For deserialization
  WearScreenPayload(Map<String, Object> delegate) {
    super(delegate);
  }

  WearScreenPayload(String category, String name, Properties properties) {
    put(CATEGORY_KEY, category);
    put(NAME_KEY, name);
    put(PROPERTIES_KEY, properties);
  }

  Properties getProperties() {
    return getValueMap(PROPERTIES_KEY, Properties.class);
  }

  String getCategory() {
    return getString(CATEGORY_KEY);
  }

  String getName() {
    return getString(NAME_KEY);
  }
}
