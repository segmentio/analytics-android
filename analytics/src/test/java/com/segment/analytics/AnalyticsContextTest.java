/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.segment.analytics.Utils.createContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.Config.NONE;

import android.content.Context;
import android.net.ConnectivityManager;
import com.google.common.collect.ImmutableMap;
import com.segment.analytics.core.BuildConfig;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class AnalyticsContextTest {

  private AnalyticsContext context;
  private Traits traits;

  @Before
  public void setUp() {
    traits = Traits.create();
    context = createContext(traits);
  }

  @Test
  public void create() {
    context = AnalyticsContext.create(RuntimeEnvironment.application, traits, true);
    assertThat(context) //
        .containsKey("app") //
        .containsKey("device") //
        .containsKey("library") //
        .containsKey("locale") //
        .containsKey("network") //
        .containsKey("os") //
        .containsKey("screen")
        .containsEntry("userAgent", "undefined") //
        .containsKey("timezone") // value depends on where the tests are run
        .containsKey("traits");

    assertThat(context.getValueMap("app")) //
        .containsEntry("name", "org.robolectric.default")
        .containsEntry("version", "undefined")
        .containsEntry("namespace", "org.robolectric.default")
        .containsEntry("build", "0");

    assertThat(context.getValueMap("device")) //
        .containsEntry("id", "unknown")
        .containsEntry("manufacturer", "unknown")
        .containsEntry("model", "unknown")
        .containsEntry("name", "unknown")
        .containsEntry("type", "android");

    assertThat(context.getValueMap("library")) //
        .containsEntry("name", "analytics-android")
        .containsEntry("version", BuildConfig.VERSION_NAME);

    // TODO: mock network state?
    assertThat(context.getValueMap("network")).isEmpty();

    assertThat(context.getValueMap("os")) //
        .containsEntry("name", "Android") //
        .containsEntry("version", "4.1.2_r1");

    assertThat(context.getValueMap("screen")) //
        .containsEntry("density", 1.5f) //
        .containsEntry("width", 480) //
        .containsEntry("height", 800);
  }

  @Test
  public void createWithoutDeviceIdCollection() {
    context = AnalyticsContext.create(RuntimeEnvironment.application, traits, false);

    assertThat(context.getValueMap("device")) //
        .containsEntry("id", traits.anonymousId())
        .containsEntry("manufacturer", "unknown")
        .containsEntry("model", "unknown")
        .containsEntry("name", "unknown")
        .containsEntry("type", "android");
  }

  @Test
  public void copyReturnsSameMappings() {
    AnalyticsContext copy = context.unmodifiableCopy();

    assertThat(copy).hasSameSizeAs(context).isNotSameAs(context).isEqualTo(context);
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      assertThat(copy).contains(MapEntry.entry(entry.getKey(), entry.getValue()));
    }
  }

  @Test
  public void copyIsImmutable() {
    AnalyticsContext copy = context.unmodifiableCopy();

    //noinspection EmptyCatchBlock
    try {
      copy.put("foo", "bar");
      fail("Inserting into copy should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {

    }
  }

  @Test
  public void traitsAreCopied() {
    assertThat(context.traits()).isEqualTo(traits).isNotSameAs(traits);

    Traits traits = new Traits().putAnonymousId("foo");
    context.setTraits(traits);
    assertThat(context.traits()).isEqualTo(traits).isNotSameAs(traits);
  }

  @Test
  public void campaign() {
    AnalyticsContext.Campaign campaign = new AnalyticsContext.Campaign();

    campaign.putName("campaign-name");
    assertThat(campaign.name()).isEqualTo("campaign-name");

    campaign.putSource("campaign-source");
    assertThat(campaign.source()).isEqualTo("campaign-source");

    campaign.putMedium("campaign-medium");
    assertThat(campaign.medium()).isEqualTo("campaign-medium");

    campaign.putTerm("campaign-term");
    assertThat(campaign.term()).isEqualTo("campaign-term");
    assertThat(campaign.tern()).isEqualTo("campaign-term");

    campaign.putContent("campaign-content");
    assertThat(campaign.content()).isEqualTo("campaign-content");

    context.putCampaign(campaign);
    assertThat(context.campaign()).isEqualTo(campaign);
  }

  @Test
  public void device() {
    AnalyticsContext.Device device = new AnalyticsContext.Device();

    device.putAdvertisingInfo("adId", true);
    assertThat(device).containsEntry("advertisingId", "adId");
    assertThat(device).containsEntry("adTrackingEnabled", true);
  }

  @Test
  public void location() {
    AnalyticsContext.Location location = new AnalyticsContext.Location();

    location.putLatitude(37.7672319);
    assertThat(location.latitude()).isEqualTo(37.7672319);

    location.putLongitude(-122.404324);
    assertThat(location.longitude()).isEqualTo(-122.404324);

    location.putSpeed(88);
    assertThat(location.speed()).isEqualTo(88);

    location.putValue("city", "San Francisco");
    assertThat(location).containsEntry("city", "San Francisco");

    context.putLocation(location);
    assertThat(context.location()).isEqualTo(location);
  }

  @Test
  public void referrer() {
    AnalyticsContext.Referrer referrer = new AnalyticsContext.Referrer();

    referrer.putId("referrer-id");
    assertThat(referrer.id()).isEqualTo("referrer-id");

    referrer.putLink("referrer-link");
    assertThat(referrer.link()).isEqualTo("referrer-link");

    referrer.putName("referrer-name");
    assertThat(referrer.name()).isEqualTo("referrer-name");

    referrer.putType("referrer-type");
    assertThat(referrer.type()).isEqualTo("referrer-type");

    referrer.putUrl("referrer-url");
    assertThat(referrer.url()).isEqualTo("referrer-url");

    context.putReferrer(referrer);
    assertThat(context).containsEntry("referrer", referrer);
  }

  @Test
  public void network() {
    Context application = mock(Context.class);
    ConnectivityManager manager = mock(ConnectivityManager.class);
    when(application.getSystemService(CONNECTIVITY_SERVICE)).thenReturn(manager);
    context.putNetwork(application);

    assertThat(context)
        .containsEntry(
            "network",
            new ImmutableMap.Builder<>()
                .put("wifi", false)
                .put("carrier", "unknown")
                .put("bluetooth", false)
                .put("cellular", false)
                .build());
  }
}
