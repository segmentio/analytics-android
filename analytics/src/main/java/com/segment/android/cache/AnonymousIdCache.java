package com.segment.android.cache;

import android.content.Context;
import com.segment.android.Constants;
import com.segment.android.utils.DeviceId;

public class AnonymousIdCache extends SimpleStringCache {

  private Context context;

  public AnonymousIdCache(Context context) {
    super(context, Constants.SharedPreferences.ANONYMOUS_ID_KEY);

    this.context = context;
  }

  @Override
  public String load() {
    return DeviceId.get(context);
  }
}
