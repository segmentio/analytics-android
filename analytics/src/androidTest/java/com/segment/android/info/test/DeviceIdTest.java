package com.segment.android.info.test;

import android.test.AndroidTestCase;
import com.segment.android.utils.DeviceId;
import junit.framework.Assert;
import org.junit.Test;

public class DeviceIdTest extends AndroidTestCase {

  @Test
  public void testGet() {
    String deviceId = DeviceId.get(this.getContext());
    Assert.assertNotNull(deviceId);
  }
}
