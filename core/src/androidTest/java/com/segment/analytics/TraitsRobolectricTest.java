package com.segment.analytics;

import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class TraitsRobolectricTest {
  Traits traits;

  @Before public void setUp() {
    traits = new Traits(Robolectric.application);
  }

  @Test public void newInvocationHasUniqueId() throws Exception {
    assertThat(traits).isNotSameAs(new Traits(Robolectric.application));
  }

  @Test public void newInvocationHasSameAnonymousAndUserId() throws Exception {
    assertThat(traits.userId()).isEqualTo(traits.anonymousId());
  }

  @Test public void publicConstructorGivesEmptyTraits() throws Exception {
    assertThat(new Traits()).hasSize(0);
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

  @Test public void firstAndLastNameSetGivesFullName() throws Exception {
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
}
