package com.segment.analytics;

import com.segment.analytics.core.BuildConfig;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.createContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class AnalyticsContextTest {

  AnalyticsContext analyticsContext;
  Traits traits;

  @Before public void setUp() {
    traits = Traits.create();
    analyticsContext = createContext(traits);
  }

  @Test public void create() {
    analyticsContext = AnalyticsContext.create(Robolectric.application, traits);
    assertThat(analyticsContext) //
        .containsKey("app") //
        .containsKey("device") //
        .containsKey("library") //
        .containsEntry("locale", "en-US") //
        .containsKey("network") //
        .containsKey("os") //
        .containsKey("screen").containsEntry("userAgent", "undefined") //
        .containsKey("timezone") // value depends on where the tests are run
        .containsKey("traits");

    assertThat(analyticsContext.getValueMap("app")) //
        .containsEntry("name", "org.robolectric.default")
        .containsEntry("version", "undefined")
        .containsEntry("namespace", "org.robolectric.default")
        .containsEntry("build", 0);

    assertThat(analyticsContext.getValueMap("device")) //
        .containsEntry("id", "unknown")
        .containsEntry("manufacturer", "unknown")
        .containsEntry("model", "unknown")
        .containsEntry("name", "unknown");

    assertThat(analyticsContext.getValueMap("library")) //
        .containsEntry("name", "analytics-android")
        .containsEntry("version", BuildConfig.VERSION_NAME);

    // TODO: mock network state?
    assertThat(analyticsContext.getValueMap("network")).isEmpty();

    assertThat(analyticsContext.getValueMap("os")) //
        .containsEntry("name", "Android") //
        .containsEntry("version", "unknown");

    assertThat(analyticsContext.getValueMap("screen")) //
        .containsEntry("density", 1.5f) //
        .containsEntry("width", 480) //
        .containsEntry("height", 800);
  }

  @Test public void copyReturnsSameMappings() {
    AnalyticsContext copy = analyticsContext.unmodifiableCopy();

    assertThat(copy).hasSameSizeAs(analyticsContext)
        .isNotSameAs(analyticsContext)
        .isEqualTo(analyticsContext);
    for (Map.Entry<String, Object> entry : analyticsContext.entrySet()) {
      assertThat(copy).contains(MapEntry.entry(entry.getKey(), entry.getValue()));
    }
  }

  @Test public void referrer() {
    String referrer = "http://segment.com?"
        + "utm_campaign=blog&"
        + "utm_medium=social&"
        + "utm_source=facebook&"
        + "utm_term=marketing+software&"
        + "utm_content=sidebarlink";
    assertThat(AnalyticsContext.Campaign.parse(referrer)).hasSize(5)
        .containsEntry("name", "blog")
        .containsEntry("medium", "social")
        .containsEntry("source", "facebook")
        .containsEntry("term", "marketing software")
        .containsEntry("content", "sidebarlink");
  }

  @Test public void copyIsImmutable() {
    AnalyticsContext copy = analyticsContext.unmodifiableCopy();

    //noinspection EmptyCatchBlock
    try {
      copy.put("foo", "bar");
      fail("Inserting into copy should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {

    }
  }

  @Test public void traitsAreCopied() {
    assertThat(analyticsContext.traits()).isEqualTo(traits).isNotSameAs(traits);

    Traits traits = new Traits().putAnonymousId("foo");
    analyticsContext.setTraits(traits);
    assertThat(analyticsContext.traits()).isEqualTo(traits).isNotSameAs(traits);
  }
}
