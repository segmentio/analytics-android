package com.segment.android.integrations.test;

import com.bugsnag.android.Bugsnag;
import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.BugsnagIntegration;
import com.segment.android.models.EasyJSONObject;
import org.junit.Test;

public class BugsnagIntegrationTest extends BaseIntegrationTest {

  @Override
  public Integration getIntegration() {
    return new BugsnagIntegration();
  }

  @Override
  public EasyJSONObject getSettings() {
    EasyJSONObject settings = new EasyJSONObject();
    settings.put("apiKey", "7563fdfc1f418e956f5e5472148759f0");
    return settings;
  }

  @Test
  public void testException() {
    reachReadyState();

    Bugsnag.notify(new RuntimeException("Non-fatal"));
  }
}

