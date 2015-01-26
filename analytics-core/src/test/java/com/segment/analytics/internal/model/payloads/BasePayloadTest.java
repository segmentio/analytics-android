package com.segment.analytics.internal.model.payloads;

import android.util.Pair;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.internal.AbstractIntegration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.createContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class BasePayloadTest {

  @Test public void payloadEnabledCorrectly() throws IOException {
    AbstractIntegration mockIntegration = mock(AbstractIntegration.class);
    when(mockIntegration.key()).thenReturn("foo");

    // this should be done with junits params, couldn't get it to work http://pastebin.com/W61q1H3J
    List<Pair<Options, Boolean>> params = new ArrayList<Pair<Options, Boolean>>();
    params.add(new Pair<Options, Boolean>(new Options(), true));

    // Respect "All" capital case
    params.add(new Pair<>(new Options().setIntegration("All", false), false));
    params.add(new Pair<>(new Options().setIntegration("All", true), true));

    // Ignore "all" under case
    params.add(new Pair<>(new Options().setIntegration("all", false), true));
    params.add(new Pair<>(new Options().setIntegration("all", true), true));

    // respect options for "foo" integration
    params.add(new Pair<>(new Options().setIntegration("foo", true), true));
    params.add(new Pair<>(new Options().setIntegration("foo", false), false));

    // ignore values for other integrations
    params.add(new Pair<>(new Options().setIntegration("bar", true), true));
    params.add(new Pair<>(new Options().setIntegration("bar", false), true));

    for (Pair<Options, Boolean> param : params) {
      AnalyticsContext analyticsContext = createContext(new Traits());
      BasePayload payload = new AliasPayload(analyticsContext, param.first, "foo");
      assertThat(payload.isIntegrationEnabledInPayload(mockIntegration)).overridingErrorMessage(
          "Expected %s for integrations %s", param.second, param.first.integrations())
          .isEqualTo(param.second);
    }
  }
}
