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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.segment.analytics.Traits.Address;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TraitsTest {

  Traits traits;

  @Before
  public void setUp() {
    traits = Traits.create();
  }

  @Test
  public void newInvocationHasUniqueId() throws Exception {
    assertThat(traits).isNotSameAs(Traits.create());
  }

  @Test
  public void newInvocationHasNoUserId() throws Exception {
    assertThat(traits.userId()).isNull();
  }

  @Test
  public void publicConstructorGivesEmptyTraits() throws Exception {
    assertThat(new Traits()).hasSize(0);
  }

  @Test
  public void userIdOrAnonymousId() throws Exception {
    assertThat(new Traits().putUserId("foo").putAnonymousId("bar").currentId()) //
        .isEqualTo("foo");
    assertThat(new Traits().putUserId("foo").currentId()).isEqualTo("foo");
    assertThat(new Traits().putAnonymousId("bar").currentId()) //
        .isEqualTo("bar");
    assertThat(new Traits().currentId()).isNull();
  }

  @Test
  public void traitsAreMergedCorrectly() throws Exception {
    Traits traits1 =
        new Traits() //
            .putAge(20)
            .putAvatar("f2prateek")
            .putDescription("the first one")
            .putLastName("Srivastava")
            .putEmail("f2prateek@gmail.com")
            .putEmployees(50);
    assertThat(traits1).hasSize(6);

    Traits traits2 =
        new Traits()
            .putAvatar("f2prateek")
            .putFirstName("Prateek")
            .putDescription("the second one");
    assertThat(traits2).hasSize(3);

    traits1.putAll(traits2);
    assertThat(traits1)
        .hasSize(7)
        .contains(MapEntry.entry("avatar", "f2prateek"))
        .contains(MapEntry.entry("description", "the second one"))
        .contains(MapEntry.entry("email", "f2prateek@gmail.com"));
    assertThat(traits1.name()).isEqualTo("Prateek Srivastava");
  }

  @Test
  public void copyReturnsSameMappings() {
    Traits copy = traits.unmodifiableCopy();

    assertThat(copy).hasSameSizeAs(traits).isNotSameAs(traits).isEqualTo(traits);
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      assertThat(copy).contains(MapEntry.entry(entry.getKey(), entry.getValue()));
    }
  }

  @Test
  public void copyIsImmutable() {
    Traits copy = traits.unmodifiableCopy();

    //noinspection EmptyCatchBlock
    try {
      copy.put("foo", "bar");
      fail("Inserting into copy should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException ignored) {
    }
  }

  @Test
  public void address() {
    Address address = new Address();

    address.putCity("Vancouver");
    assertThat(address.city()).isEqualTo("Vancouver");

    address.putCountry("Canada");
    assertThat(address.country()).isEqualTo("Canada");

    address.putPostalCode("V6R 1J3");
    assertThat(address.postalCode()).isEqualTo("V6R 1J3");

    address.putState("BC");
    assertThat(address.state()).isEqualTo("BC");

    address.putStreet("128 W Hastings");
    assertThat(address.street()).isEqualTo("128 W Hastings");

    traits.putAddress(address);
    assertThat(traits.address()).isEqualTo(address);
  }

  @Test
  public void age() {
    traits.putAge(25);
    assertThat(traits.age()).isEqualTo(25);
  }

  @Test
  public void avatar() {
    traits.putAvatar("https://github.com/identicons/segmentio.png");
    assertThat(traits.avatar()).isEqualTo("https://github.com/identicons/segmentio.png");
  }

  @Test
  public void emptyBirthdayDoesNotCrash() {
    // Exercise a bug where trying to fetch the birthday when one didn't exist would crash.
    assertThat(traits.birthday()).isNull();
  }

  @Test
  public void birthday() {
    Date date = new GregorianCalendar(1992, 2, 10).getTime();
    traits.putBirthday(date);
    assertThat(traits.birthday()).isEqualTo(date);
  }

  @Test
  public void createdAt() {
    traits.putCreatedAt("16-02-217");
    assertThat(traits.createdAt()).isEqualTo("16-02-217");
  }

  @Test
  public void description() {
    traits.putDescription("a really amazing library");
    assertThat(traits.description()).isEqualTo("a really amazing library");
  }

  @Test
  public void email() {
    traits.putEmail("prateek@segment.com");
    assertThat(traits.email()).isEqualTo("prateek@segment.com");
  }

  @Test
  public void employees() {
    traits.putEmployees(1000);
    assertThat(traits.employees()).isEqualTo(1000);
  }

  @Test
  public void fax() {
    traits.putFax("123-456-7890");
    assertThat(traits.fax()).isEqualTo("123-456-7890");
  }

  @Test
  public void gender() {
    traits.putGender("male");
    assertThat(traits.gender()).isEqualTo("male");
  }

  @Test
  public void industry() {
    traits.putIndustry("SAAS");
    assertThat(traits.industry()).isEqualTo("SAAS");
  }

  @Test
  public void name() {
    assertThat(traits.name()).isNull();

    traits.putFirstName("prateek");
    traits.putLastName("srivastava");
    assertThat(traits.name()).isEqualTo("prateek srivastava");

    traits.putName("mr. prateek srivastava");
    assertThat(traits.name()).isEqualTo("mr. prateek srivastava");
  }

  @Test
  public void firstName() {
    traits.putFirstName("prateek");
    assertThat(traits.name()).isEqualTo("prateek");
    assertThat(traits.firstName()).isEqualTo("prateek");
  }

  @Test
  public void lastName() {
    traits.putLastName("srivastava");
    assertThat(traits.name()).isEqualTo("srivastava");
    assertThat(traits.lastName()).isEqualTo("srivastava");
  }

  @Test
  public void phone() {
    traits.putPhone("123-456-7890");
    assertThat(traits.phone()).isEqualTo("123-456-7890");
  }

  @Test
  public void title() {
    traits.putTitle("Software Engineer");
    assertThat(traits.title()).isEqualTo("Software Engineer");
  }

  @Test
  public void username() {
    traits.putUsername("f2prateek");
    assertThat(traits.username()).isEqualTo("f2prateek");
  }

  @Test
  public void website() {
    traits.putWebsite("https://segment.com/");
    assertThat(traits.website()).isEqualTo("https://segment.com/");
  }
}
