package com.segment.analytics.internal.model.payloads;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.integrations.BasePayload;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class BasePayloadTest {

  @Test public void newInvocationIsCreatedWithDefaults() {
    AnalyticsContext analyticsContext = mock(AnalyticsContext.class);
    when(analyticsContext.unmodifiableCopy()).thenReturn(analyticsContext);
    when(analyticsContext.traits()).thenReturn(mock(Traits.class));

    RealPayload realPayload =
        new RealPayload(BasePayload.Type.alias, analyticsContext, new Options());

    assertThat(realPayload).containsEntry("type", BasePayload.Type.alias);
    assertThat(realPayload).containsEntry("type", BasePayload.Type.alias);
    assertThat(realPayload).containsEntry("type", BasePayload.Type.alias);
  }

  static class RealPayload extends BasePayload {

    public RealPayload(Type type, AnalyticsContext context, Options options) {
      super(type, context, options);
    }
  }
}
