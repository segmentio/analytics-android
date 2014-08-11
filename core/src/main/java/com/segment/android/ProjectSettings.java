package com.segment.android;

import com.segment.android.json.JsonMap;

class ProjectSettings extends SegmentEntity<ProjectSettings> {
  ProjectSettings(String json) {
    super(json);
  }

  @Override protected ProjectSettings self() {
    return this;
  }

  JsonMap getSettingsForIntegration(Integration integration) {
    return getJsonMap(integration.getKey());
  }
}
