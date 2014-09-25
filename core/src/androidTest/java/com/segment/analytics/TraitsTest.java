package com.segment.analytics;

import org.fest.assertions.data.MapEntry;

import static org.fest.assertions.api.Assertions.assertThat;

public class TraitsTest extends BaseAndroidTestCase {
  Traits traits;

  @Override public void setUp() throws Exception {
    super.setUp();
    traits = new Traits(getContext());
  }

  public void testNewInvocationHasUniqueId() throws Exception {
    assertThat(traits).isNotSameAs(new Traits(getContext()));
  }

  public void testNewInvocationHasSameAnonymousAndUserId() throws Exception {
    assertThat(traits.userId()).isEqualTo(traits.anonymousId());
  }

  public void testPublicConstructorGivesEmptyTraits() throws Exception {
    assertThat(new Traits()).hasSize(0);
  }

  public void testTraitsMerged() throws Exception {
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

    traits1.merge(traits2);
    assertThat(traits1).hasSize(7)
        .contains(MapEntry.entry("avatar", "f2prateek"))
        .contains(MapEntry.entry("description", "the second one"))
        .contains(MapEntry.entry("email", "f2prateek@gmail.com"));
    assertThat(traits1.name()).isEqualTo("Prateek Srivastava");
  }

  public void testFirstAndLastNameSetGivesFullName() throws Exception {
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
