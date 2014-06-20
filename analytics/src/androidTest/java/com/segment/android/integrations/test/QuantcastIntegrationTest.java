package com.segment.android.integrations.test;

import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.QuantcastIntegration;
import com.segment.android.models.EasyJSONObject;

public class QuantcastIntegrationTest extends BaseIntegrationTest {

  @Override
  public Integration getIntegration() {
    return new QuantcastIntegration();
  }

  @Override
  public EasyJSONObject getSettings() {
    EasyJSONObject settings = new EasyJSONObject();
    settings.put("apiKey", "0ni8b9kcadbkirvq-7hqqc0yfvgqb6mk2");
    return settings;
  }
}