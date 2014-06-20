package com.segment.android.models;

import org.json.JSONObject;

public class Traits extends EasyJSONObject {

  public Traits() {
  }

  public Traits(Object... kvs) {
    super(kvs);
  }

  public Traits(JSONObject json) {
    super(json);
  }
}
