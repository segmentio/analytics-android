package com.segment.android;

class ProjectSettings extends Json<ProjectSettings> {
  ProjectSettings(String json) {
    super(json);
  }

  @Override protected ProjectSettings self() {
    return this;
  }

  Json getSettingsForIntegration(Integration integration) {
    return getJson(integration.getKey());
  }
}
