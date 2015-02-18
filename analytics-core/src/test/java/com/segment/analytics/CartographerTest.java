package com.segment.analytics;

import com.segment.analytics.internal.model.payloads.BasePayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.createContext;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class CartographerTest {

  static String json;
  static TrackPayload trackPayload;

  static {
    json = "{\""
        + "messageId\":\"a161304c-498c-4830-9291-fcfb8498877b\","
        + "\"type\":\"track\","
        + "\"channel\":\"mobile\","
        + "\"context\":{\"traits\":{}},"
        + "\"anonymousId\":null,"
        + "\"timestamp\":\"2014-12-15T13:32:44-0700\","
        + "\"integrations\":"
        + "{\"All\":true},"
        + "\"event\":\"foo\","
        + "\"properties\":{}"
        + "}";

    AnalyticsContext analyticsContext = createContext(new Traits());
    trackPayload = new TrackPayload(analyticsContext, new Options(), "foo", new Properties());
    // put some predictable values for automatically generated data
    trackPayload.put("messageId", "a161304c-498c-4830-9291-fcfb8498877b");
    trackPayload.put("timestamp", "2014-12-15T13:32:44-0700");
  }

  @Test public void testSerialization() throws IOException {
    assertThat(Cartographer.INSTANCE.toJson(trackPayload)).isEqualTo(json);
  }

  @Test public void testDeserialization() throws IOException {
    Map<String, Object> map = Cartographer.INSTANCE.fromJson(json);

    // special consideration for enums
    assertThat(map).containsEntry("type", "track").containsEntry("channel", "mobile");
    map.put("type", BasePayload.Type.track);
    map.put("channel", BasePayload.Channel.mobile);

    assertThat(map).isEqualTo(trackPayload);
  }
}
