package com.segment.android.integrations.test;

import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.GoogleAnalyticsIntegration;
import com.segment.android.models.EasyJSONObject;

public class GoogleAnalyticsIntegrationTest extends BaseIntegrationTest {

  @Override
  public Integration getIntegration() {
    return new GoogleAnalyticsIntegration();
  }

  @Override
  public EasyJSONObject getSettings() {
    EasyJSONObject settings = new EasyJSONObject();
    settings.put("mobileTrackingId", "UA-27033709-9");
    settings.put("anonymizeIp", false);
    settings.put("reportUncaughtExceptions", true);
    settings.put("mobileHttps", true);
    return settings;
  }
}