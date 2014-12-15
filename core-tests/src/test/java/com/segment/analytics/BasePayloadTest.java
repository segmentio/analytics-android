package com.segment.analytics;

import android.util.Pair;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class BasePayloadTest {

  @Mock AbstractIntegration mockIntegration;

  @Before public void setUp() {
    initMocks(this);
    when(mockIntegration.key()).thenReturn("foo");
  }

  @Test public void payloadEnabledCorrectly() throws IOException {
    // this should be done with junits params, couldn't get it to work http://pastebin.com/W61q1H3J
    List<Pair<Options, Boolean>> params = new ArrayList<Pair<Options, Boolean>>();
    params.add(new Pair<Options, Boolean>(new Options(), true));

    // Respect "All" capital case
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("All", false), false));
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("All", true), true));

    // Ignore "all" under case
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("all", false), true));
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("all", true), true));

    // respect options for "foo" integration
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("foo", true), true));
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("foo", false), false));

    // ignore values for other integrations
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("bar", true), true));
    params.add(new Pair<Options, Boolean>(new Options().setIntegration("bar", false), true));

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
  }

  private static <T extends ValueMap> T createValueMap(Map map, Class<T> clazz) {
    try {
      Constructor<T> constructor = clazz.getDeclaredConstructor(Map.class);
      constructor.setAccessible(true);
      return constructor.newInstance(map);
    } catch (Exception e) {
      throw new RuntimeException("Could not create instance of " + clazz.getCanonicalName(), e);
    }
  }
}
