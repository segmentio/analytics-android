package com.segment.android.cache;

import android.os.Handler;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.request.IRequester;
import com.segment.android.utils.LooperThreadWithHandler;

/**
 * A Looper/Handler backed settings thread
 */
public class SettingsThread extends LooperThreadWithHandler implements ISettingsLayer {

  private IRequester requester;

  public SettingsThread(IRequester requester) {
    this.requester = requester;
  }

  @Override
  public void fetch(final SettingsCallback callback) {
    Handler handler = handler();
    handler.post(new Runnable() {
      @Override
      public void run() {
        EasyJSONObject object = requester.fetchSettings();
        if (callback != null) callback.onSettingsLoaded(object != null, object);
      }
    });
  }
}