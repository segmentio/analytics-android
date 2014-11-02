package com.segment.analytics;

import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.Mock;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Tapstream.class)
public class TapstreamRobolectricTest extends IntegrationRobolectricExam {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Tapstream tapstream;
  @Mock com.tapstream.sdk.Config config;
  TapstreamIntegrationAdapter adapter;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Tapstream.class);
    adapter = new TapstreamIntegrationAdapter(true);
    adapter.tapstream = tapstream;
    adapter.config = config;
  }

  @Test public void initialize() throws InvalidConfigurationException {
    TapstreamIntegrationAdapter adapter = new TapstreamIntegrationAdapter(true);
    adapter.initialize(context, new JsonMap().putValue("accountName", "foo")
        .putValue("sdkSecret", "bar")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true));
    verifyStatic();
    Tapstream.create(eq(context), eq("foo"), eq("bar"), Matchers.<com.tapstream.sdk.Config>any());
  }

  @Test public void track() {
    adapter.track(trackPayload("foo"));
    verify(tapstream).fireEvent(Matchers.<Event>any());
  }

  @Test public void identify() {
    Map<String, Object> map = new HashMap<String, Object>();
    config.globalEventParams = map;
    IdentifyPayload payload = identifyPayload("foo");
    adapter.identify(payload);
    assertThat(map).hasSize(0);
  }

  @Test public void identifyWithTraits() {
    Map<String, Object> map = new HashMap<String, Object>();
    config.globalEventParams = map;
    traits.putValue("foo", "bar");
    traits.putValue("baz", "qux");
    IdentifyPayload payload = identifyPayload("foo");
    adapter.identify(payload);
    assertThat(map).hasSize(2)
        .containsExactly(MapEntry.entry("foo", "bar"), MapEntry.entry("baz", "qux"));
  }
}
