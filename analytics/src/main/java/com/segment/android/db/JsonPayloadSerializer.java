package com.segment.android.db;

import android.util.Log;
import com.segment.android.Logger;
import com.segment.android.models.Alias;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonPayloadSerializer implements IPayloadSerializer {

  @Override
  public String serialize(BasePayload payload) {
    return payload.toString();
  }

  @Override
  public BasePayload deserialize(String str) {
    JSONObject obj;
    try {
      obj = new JSONObject(str);
      String type = obj.getString("type");
      if (type.equals(Identify.TYPE)) {
        return new Identify(obj);
      } else if (type.equals(Track.TYPE)) {
        return new Track(obj);
      } else if (type.equals(Alias.TYPE)) {
        return new Alias(obj);
      } else if (type.equals(Group.TYPE)) {
        return new Group(obj);
      } else if (type.equals(Screen.TYPE)) {
        return new Screen(obj);
      } else {
        Logger.e("Failed to convert json to base payload because of unknown type: " + type);
      }
    } catch (JSONException e) {
      Logger.e("Failed to convert json to base payload: " + Log.getStackTraceString(e));
    }

    return null;
  }
}
