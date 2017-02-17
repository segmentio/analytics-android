package com.segment.analytics;

import com.segment.analytics.core.BuildConfig;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Utils.createContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = NONE)
public class AnalyticsContextTest {
  AnalyticsContext context;
  Traits traits;

  @Before public void setUp() {
    traits = Traits.create();
    context = createContext(traits);
  }

  @Test public void create() {
    context = AnalyticsContext.create(RuntimeEnvironment.application, traits, true);
    assertThat(context) //
        .containsKey("app") //
        .containsKey("device") //
        .containsKey("library") //
        .containsEntry("locale", "en-US") //
        .containsKey("network") //
        .containsKey("os") //
        .containsKey("screen").containsEntry("userAgent", "undefined") //
        .containsKey("timezone") // value depends on where the tests are run
        .containsKey("traits");

    assertThat(context.getValueMap("app")) //
        .containsEntry("name", "com.segment.analytics.core")
        .containsEntry("version", "undefined")
        .containsEntry("namespace", "com.segment.analytics.core")
        .containsEntry("build", 0);

    assertThat(context.getValueMap("device")) //
        .containsEntry("id", "unknown")
        .containsEntry("manufacturer", "unknown")
        .containsEntry("model", "unknown")
        .containsEntry("name", "unknown");

    assertThat(context.getValueMap("library")) //
        .containsEntry("name", "analytics-android")
        .containsEntry("version", BuildConfig.VERSION_NAME);

    // TODO: mock network state?
    assertThat(context.getValueMap("network")).isEmpty();

    assertThat(context.getValueMap("os")) //
        .containsEntry("name", "Android") //
        .containsEntry("version", "4.3_r2");

    assertThat(context.getValueMap("screen")) //
        .containsEntry("density", 1.5f) //
        .containsEntry("width", 480) //
        .containsEntry("height", 800);

    // disable device id collection
    context = AnalyticsContext.create(RuntimeEnvironment.application, traits, false);

    assertThat(context.getValueMap("device")) //
        .containsEntry("id", traits.anonymousId())
        .containsEntry("manufacturer", "unknown")
        .containsEntry("model", "unknown")
        .containsEntry("name", "unknown");
  }

  @Test public void copyReturnsSameMappings() {
    AnalyticsContext copy = context.unmodifiableCopy();

    assertThat(copy).hasSameSizeAs(context).isNotSameAs(context).isEqualTo(context);
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      assertThat(copy).contains(MapEntry.entry(entry.getKey(), entry.getValue()));
    }
  }

  @Test public void copyIsImmutable() {
    AnalyticsContext copy = context.unmodifiableCopy();

    //noinspection EmptyCatchBlock
    try {
      copy.put("foo", "bar");
      fail("Inserting into copy should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {

    }
  }

  @Test public void traitsAreCopied() {
    assertThat(context.traits()).isEqualTo(traits).isNotSameAs(traits);

    Traits traits = new Traits().putAnonymousId("foo");
    context.setTraits(traits);
    assertThat(context.traits()).isEqualTo(traits).isNotSameAs(traits);
  }

  @Test public void campaign() {
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

    campaign.putTerm("campaign-content");
    assertThat(campaign.term()).isEqualTo("campaign-content");

    context.putCampaign(campaign);
    assertThat(context.campaign()).isEqualTo(campaign);
  }

  @Test public void device() {
    AnalyticsContext.Device device = new AnalyticsContext.Device();

    device.putAdvertisingInfo("adId", true);
    assertThat(device).containsEntry("advertisingId", "adId");
    assertThat(device).containsEntry("adTrackingEnabled", true);
  }

  @Test public void location() {
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

  @Test public void referrer() {
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
}
