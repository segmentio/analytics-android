package com.segment.android.info;

import android.content.Context;
import com.segment.android.models.EasyJSONObject;
import org.json.JSONObject;

public class OS implements Info<JSONObject> {

  @Override
  public String getKey() {
    return "os";
  }

  @Override
  public JSONObject get(Context context) {
    EasyJSONObject os = new EasyJSONObject();
    os.put("name", "Android");
    os.put("version", android.os.Build.VERSION.SDK_INT);
    return os;
  }
}
