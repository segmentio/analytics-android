package com.segment.android.info.test;

import android.test.AndroidTestCase;
import com.segment.android.Config;
import com.segment.android.info.InfoManager;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class InfoManagerTest extends AndroidTestCase {

  @Test
  public void testGet() {
    InfoManager manager = new InfoManager(new Config());
    JSONObject object = manager.build(this.getContext());
    Assert.assertTrue(object.length() > 0);
  }
}
