package com.segment.android.integrations.test;

import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.FlurryIntegration;
import com.segment.android.models.EasyJSONObject;

public class FlurryIntegrationTest extends BaseIntegrationTest {

  @Override
  public Integration getIntegration() {
    return new FlurryIntegration();
  }

  @Override
  public EasyJSONObject getSettings() {
    EasyJSONObject settings = new EasyJSONObject();
    settings.put("apiKey", "X4Z6T4FZFCQF6TNGMZ93");
    settings.put("sessionLength", 30);
    settings.put("captureUncaughtExceptions", true);
    settings.put("useHttps", true);
    return settings;
  }
}