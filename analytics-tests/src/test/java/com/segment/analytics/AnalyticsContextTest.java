package com.segment.analytics;

import com.segment.analytics.core.tests.BuildConfig;
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

  AnalyticsContext analyticsContext;
  Traits traits;

  @Before public void setUp() {
    traits = Traits.create();
    analyticsContext = createContext(traits);
  }

  @Test public void create() {
    analyticsContext = AnalyticsContext.create(RuntimeEnvironment.application, traits, true);
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
        .containsEntry("name", "com.segment.analytics.core.tests")
        .containsEntry("version", "undefined")
        .containsEntry("namespace", "com.segment.analytics.core.tests")
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
        .containsEntry("version", "4.3_r2");

    assertThat(analyticsContext.getValueMap("screen")) //
        .containsEntry("density", 1.5f) //
        .containsEntry("width", 480) //
        .containsEntry("height", 800);

    // disable device id collection
    analyticsContext = AnalyticsContext.create(RuntimeEnvironment.application, traits, false);

    assertThat(analyticsContext.getValueMap("device")) //
        .containsEntry("id", traits.anonymousId())
        .containsEntry("manufacturer", "unknown")
        .containsEntry("model", "unknown")
        .containsEntry("name", "unknown");
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
