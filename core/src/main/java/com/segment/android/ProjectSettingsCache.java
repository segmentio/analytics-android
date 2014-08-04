package com.segment.android;

import android.content.Context;

import static com.segment.android.Utils.getSharedPreferences;

class ProjectSettingsCache {
  private final StringCache cache;

  ProjectSettingsCache(Context context) {
    cache = new StringCache(getSharedPreferences(context), "project_settings");
  }

  ProjectSettings get() {
    return new ProjectSettings(cache.get());
  }

  boolean isSet() {
    return cache.isSet();
  }

  void set(ProjectSettings value) {
    cache.set(value.toString());
  }

  void delete() {
    cache.delete();
  }
}
