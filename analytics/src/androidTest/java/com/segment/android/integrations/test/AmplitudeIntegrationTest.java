package com.segment.android.integrations.test;

import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.AmplitudeIntegration;
import com.segment.android.models.EasyJSONObject;

public class AmplitudeIntegrationTest extends BaseIntegrationTest {

  @Override
  public Integration getIntegration() {
    return new AmplitudeIntegration();
  }

  @Override
  public EasyJSONObject getSettings() {
    EasyJSONObject settings = new EasyJSONObject();
    settings.put("apiKey", "07808866adb2510adf19ee69e8fc2201");
    return settings;
  }
}
