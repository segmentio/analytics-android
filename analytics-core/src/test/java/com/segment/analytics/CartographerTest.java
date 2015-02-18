package com.segment.analytics;

import com.segment.analytics.internal.model.payloads.BasePayload;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.TRACK_PAYLOAD;
import static com.segment.analytics.TestUtils.TRACK_PAYLOAD_JSON;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class CartographerTest {

  @Test public void testSerialization() throws IOException {
    assertThat(Cartographer.INSTANCE.toJson(TRACK_PAYLOAD)).isEqualTo(TRACK_PAYLOAD_JSON);
  }

  @Test public void testDeserialization() throws IOException {
    Map<String, Object> map = Cartographer.INSTANCE.fromJson(TRACK_PAYLOAD_JSON);

    // special consideration for enums
    assertThat(map).containsEntry("type", "track").containsEntry("channel", "mobile");
    map.put("type", BasePayload.Type.track);
    map.put("channel", BasePayload.Channel.mobile);

    assertThat(map).isEqualTo(TRACK_PAYLOAD);
  }
}
