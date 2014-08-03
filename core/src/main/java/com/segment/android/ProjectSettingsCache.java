package com.segment.android;

import android.content.Context;

import static com.segment.android.Utils.getSharedPreferences;

class ProjectSettingsCache {
  private final StringCache cache;

  public ProjectSettingsCache(Context context) {
    cache = new StringCache(getSharedPreferences(context), "project_settings");
  }

  public ProjectSettings get() {
    return new ProjectSettings(cache.get());
  }

  public boolean isSet() {
    return cache.isSet();
  }

  public void set(ProjectSettings value) {
    cache.set(value.toString());
  }

  public void delete() {
    cache.delete();
  }
}
