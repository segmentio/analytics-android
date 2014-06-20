package com.segment.android.info;

import android.content.Context;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.utils.DeviceId;
import org.json.JSONObject;

public class Device implements Info<JSONObject> {

  @Override
  public String getKey() {
    return "device";
  }

  @Override
  public JSONObject get(Context context) {
    EasyJSONObject device = new EasyJSONObject();

    device.put("id", DeviceId.get(context));
    device.put("manufacturer", android.os.Build.MANUFACTURER);
    device.put("model", android.os.Build.MODEL);
    device.put("version", android.os.Build.VERSION.SDK_INT);

    return device;
  }
}
