package com.segment.analytics;

import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.createContext;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class CartographerTest {

  @Test public void testSerialization() throws IOException {
    AnalyticsContext analyticsContext = createContext(new Traits());
    TrackPayload trackPayload =
        new TrackPayload(analyticsContext, new Options(), "foo", new Properties());

    // put some predictable values for randomly automatically data
    trackPayload.put("messageId", "a161304c-498c-4830-9291-fcfb8498877b");
    trackPayload.put("timestamp", "2014-12-15T13:32:44-0700");

    String json = Cartographer.INSTANCE.toJson(trackPayload);

    assertThat(json).isEqualTo("{\""
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
        + "}");
  }
}
