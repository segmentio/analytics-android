package com.segment.analytics.internal.model.payloads;

import android.util.Pair;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.JsonUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.createValueMap;
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
      Traits traits = Traits.create(Robolectric.application);
      AnalyticsContext analyticsContext = new AnalyticsContext(Robolectric.application, traits);
      BasePayload payload = new AliasPayload(analyticsContext, param.first, "foo");
      assertThat(payload.isIntegrationEnabledInPayload(mockIntegration)).overridingErrorMessage(
          "Expected %s for integrations %s", param.second, param.first.integrations())
          .isEqualTo(param.second);
    }
  }

  static class FakePayload extends BasePayload {
    FakePayload(Type type, AnalyticsContext context, Options options) {
      super(type, context, options);
    }

    @Override public void run(AbstractIntegration integration) {

    }
  }

  @Test public void payloadCreatedCorrectly() throws IOException {
    AnalyticsContext analyticsContext =
        createValueMap(new LinkedHashMap<String, Object>(), AnalyticsContext.class);
    Traits traits = new Traits().putUserId("foo").putAnonymousId("bar").putAge(20);
    analyticsContext.setTraits(traits);
    FakePayload fakePayload =
        new FakePayload(BasePayload.Type.alias, analyticsContext, new Options());

    assertThat(fakePayload) //
        .contains(MapEntry.entry("type", BasePayload.Type.alias))
        .contains(MapEntry.entry("channel", BasePayload.Channel.mobile))
        .contains(MapEntry.entry("anonymousId", "bar"))
        .contains(MapEntry.entry("userId", "foo"))
        .containsKey("integrations")
        .containsKey("context")
        .containsKey("messageId")
        .containsKey("timestamp");

    // It should have the same mappings but be a copy (and hence a different instance)
    assertThat(fakePayload.context()).isNotSameAs(analyticsContext).isEqualTo(analyticsContext);
    assertThat(fakePayload.context().traits()).isNotSameAs(traits).isEqualTo(traits);

    // put some predictable values for automatically generated data
    fakePayload.put("messageId", "a161304c-498c-4830-9291-fcfb8498877b");
    fakePayload.put("timestamp", "2014-12-15T13:32:44-0700");

    String json = JsonUtils.mapToJson(fakePayload);
    assertThat(json).isEqualTo("{\""
        + "messageId\":\"a161304c-498c-4830-9291-fcfb8498877b\",\""
        + "type\":\"alias\",\""
        + "channel\":\"mobile\",\""
        + "context\":{\""
        + "traits\":{\""
        + "userId\":\"foo\",\""
        + "anonymousId\":\"bar\",\""
        + "age\":20}},\""
        + "anonymousId\":\"bar\",\""
        + "userId\":\"foo\",\""
        + "timestamp\":\"2014-12-15T13:32:44-0700\",\""
        + "integrations\":{\""
        + "All\":true"
        + "}"
        + "}");
  }
}
