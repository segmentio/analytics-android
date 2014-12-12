package com.segment.analytics;

import android.util.Pair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class BasePayloadRobolectricTest {

  @Mock AbstractIntegration mockIntegration;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mockIntegration.key()).thenReturn("foo");
  }

  @Test public void defaultPayloadRunsOnAllIntegrations() throws IOException {
    // this should be done with junits params, couldn't get it to work http://pastebin.com/W61q1H3J
    List<Pair<Options, Boolean>> params = new ArrayList<Pair<Options, Boolean>>();
    params.add(new Pair<Options, Boolean>(new Options(), true));
    // respect integration specific options
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("foo", true), true));
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("foo", false), false));

    // ignores "All" capital case
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("All", false), true));
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("All", true), true));

    // Respect "all" under case
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("all", false), false));
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("all", true), true));

    // ignore values for other integrations
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("bar", true), true));
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("bar", false), true));

    for (Pair<Options, Boolean> param : params) {
      BasePayload payload =
          new AliasPayload("qaz", mock(AnalyticsContext.class), "qux", "baaz", param.first);
      assertThat(payload.isIntegrationEnabledInPayload(mockIntegration)).overridingErrorMessage(
          "Expected %s for integrations %s", param.second, param.first.integrations())
          .isEqualTo(param.second);
    }
  }
}
