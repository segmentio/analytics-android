package com.segment.android.info;

import android.content.Context;

public class UserAgent implements Info<String> {

  @Override
  public String getKey() {
    return "userAgent";
  }

  @Override
  public String get(Context context) {
    // http://stackoverflow.com/questions/6824604/
    // how-to-get-the-default-http-user-agent-from-the-android-device
    return System.getProperty("http.agent");
  }
}
