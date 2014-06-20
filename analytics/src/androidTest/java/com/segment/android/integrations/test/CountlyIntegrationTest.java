package com.segment.android.integrations.test;

import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.CountlyIntegration;
import com.segment.android.models.EasyJSONObject;

public class CountlyIntegrationTest extends BaseIntegrationTest {

  @Override
  public Integration getIntegration() {
    return new CountlyIntegration();
  }

  @Override
  public EasyJSONObject getSettings() {
    EasyJSONObject settings = new EasyJSONObject();
    settings.put("appKey", "caf520b7eec55e3f5da0a8739bc9208397335832");
    settings.put("serverUrl", "https://cloud.count.ly");
    return settings;
  }
}
