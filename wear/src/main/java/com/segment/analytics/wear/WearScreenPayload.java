package com.segment.analytics.wear;

import com.segment.analytics.Properties;
import com.segment.analytics.json.JsonMap;

class WearScreenPayload extends JsonMap {
  private static final String CATEGORY_KEY = "category";
  private static final String NAME_KEY = "name";
  private static final String PROPERTIES_KEY = "properties";

  WearScreenPayload(String json) {
    super(json);
  }

  WearScreenPayload(String category, String name, Properties properties) {
    put(CATEGORY_KEY, category);
    put(NAME_KEY, name);
    put(PROPERTIES_KEY, properties);
  }

  Properties getProperties() {
    return getJsonMap(PROPERTIES_KEY, Properties.class);
  }

  String getCategory() {
    return getString(CATEGORY_KEY);
  }

  String getName() {
    return getString(NAME_KEY);
  }
}
