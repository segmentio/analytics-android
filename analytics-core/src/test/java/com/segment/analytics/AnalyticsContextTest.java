package com.segment.analytics;

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
  Traits traits = Traits.create(Robolectric.application);

  @Before public void setUp() {
    analyticsContext = createContext(traits);
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
