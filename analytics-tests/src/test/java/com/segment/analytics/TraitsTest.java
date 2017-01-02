package com.segment.analytics;

import com.segment.analytics.core.tests.BuildConfig;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = NONE)
public class TraitsTest {

  Traits traits;

  @Before public void setUp() {
    traits = Traits.create();
  }

  @Test public void newInvocationHasUniqueId() throws Exception {
    assertThat(traits).isNotSameAs(Traits.create());
  }

  @Test public void newInvocationHasNoUserId() throws Exception {
    assertThat(traits.userId()).isNull();
  }

  @Test public void publicConstructorGivesEmptyTraits() throws Exception {
    assertThat(new Traits()).hasSize(0);
  }

  @Test public void userIdOrAnonymousId() throws Exception {
    assertThat(new Traits().putUserId("foo").putAnonymousId("bar").currentId()) //
        .isEqualTo("foo");
    assertThat(new Traits().putUserId("foo").currentId()).isEqualTo("foo");
    assertThat(new Traits().putAnonymousId("bar").currentId()) //
        .isEqualTo("bar");
    assertThat(new Traits().currentId()).isNull();
  }

  @Test public void traitsAreMergedCorrectly() throws Exception {
    Traits traits1 = new Traits() //
        .putAge(20)
        .putAvatar("f2prateek")
        .putDescription("the first one")
        .putLastName("Srivastava")
        .putEmail("f2prateek@gmail.com")
        .putEmployees(50);
    assertThat(traits1).hasSize(6);

    Traits traits2 = new Traits().putAvatar("f2prateek")
        .putFirstName("Prateek")
        .putDescription("the second one");
    assertThat(traits2).hasSize(3);

    traits1.putAll(traits2);
    assertThat(traits1).hasSize(7)
        .contains(MapEntry.entry("avatar", "f2prateek"))
        .contains(MapEntry.entry("description", "the second one"))
        .contains(MapEntry.entry("email", "f2prateek@gmail.com"));
    assertThat(traits1.name()).isEqualTo("Prateek Srivastava");
  }

  @Test public void copyReturnsSameMappings() {
    Traits copy = traits.unmodifiableCopy();

    assertThat(copy).hasSameSizeAs(traits).isNotSameAs(traits).isEqualTo(traits);
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      assertThat(copy).contains(MapEntry.entry(entry.getKey(), entry.getValue()));
    }
  }

  @Test public void copyIsImmutable() {
    Traits copy = traits.unmodifiableCopy();

    //noinspection EmptyCatchBlock
    try {
      copy.put("foo", "bar");
      fail("Inserting into copy should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException ignored) {
    }
  }

  @Test public void name() {
    assertThat(traits.name()).isNull();

    traits.putFirstName("prateek");
    traits.putLastName("srivastava");
    assertThat(traits.name()).isEqualTo("prateek srivastava");

    traits.clear();
    traits.putFirstName("prateek");
    assertThat(traits.name()).isEqualTo("prateek");

    traits.clear();
    traits.putLastName("srivastava");
    assertThat(traits.name()).isEqualTo("srivastava");
  }

  @Test public void emptyBirthdayDoesNotCrash() {
    // Exercise a bug where trying to fetch the birthday when one didn't exist would crash.
    assertThat(traits.birthday()).isNull();
  }
}
