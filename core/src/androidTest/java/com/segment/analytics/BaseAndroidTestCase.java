package com.segment.analytics;

import android.test.AndroidTestCase;

import static org.mockito.MockitoAnnotations.initMocks;

public class BaseAndroidTestCase extends AndroidTestCase {
  @Override protected void setUp() throws Exception {
    super.setUp();
    System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
    initMocks(this);
  }
}
