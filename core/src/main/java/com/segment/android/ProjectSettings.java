package com.segment.android;

import com.segment.android.json.JsonMap;

class ProjectSettings extends JsonMap {
  ProjectSettings(String json) {
    super(json);
  }

  JsonMap getSettingsForIntegration(Integration integration) {
    return getJsonMap(integration.getKey());
  }
}
