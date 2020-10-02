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
package com.segment.analytics

import java.lang.Exception
import java.util.GregorianCalendar
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.MapEntry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TraitsTest {
    lateinit var traits: Traits

    @Before
    fun setUp() {
        traits = Traits.create()
    }

    @Test
    @Throws(Exception::class)
    fun newInvocationHasUniqueId() {
        assertThat(traits).isNotSameAs(Traits.create())
    }

    @Test
    @Throws(Exception::class)
    fun newInvocationHasNoUserId() {
        assertThat(traits.userId()).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun publicConstructorGivesEmptyTraits() {
        assertThat(Traits()).hasSize(0)
    }

    @Test
    @Throws(Exception::class)
    fun userIdOrAnonymousId() {
        assertThat(Traits().putUserId("foo").putAnonymousId("bar").currentId())
            .isEqualTo("foo")
        assertThat(Traits().putUserId("foo").currentId()).isEqualTo("foo")
        assertThat(Traits().putAnonymousId("bar").currentId()).isEqualTo("bar")
        assertThat(Traits().currentId()).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun traitsAreMergedCorrectly() {
        val traits1 = Traits()
            .putAge(20)
            .putAvatar("f2prateek")
            .putDescription("the first one")
            .putLastName("Srivastava")
            .putEmail("f2prateek@gmail.com")
            .putEmployees(50)
        assertThat(traits1).hasSize(6)

        val traits2 = Traits()
            .putAvatar("f2prateek")
            .putFirstName("Prateek")
            .putDescription("the second one")
        assertThat(traits2).hasSize(3)

        traits1.putAll(traits2)
        assertThat(traits1)
            .hasSize(7)
            .contains(MapEntry.entry("avatar", "f2prateek"))
            .contains(MapEntry.entry("description", "the second one"))
            .contains(MapEntry.entry("email", "f2prateek@gmail.com"))
        assertThat(traits1.name()).isEqualTo("Prateek Srivastava")
    }

    @Test
    fun copyReturnsSameMappings() {
        val copy = traits.unmodifiableCopy()
        assertThat(copy).hasSameSizeAs(traits).isNotSameAs(traits).isEqualTo(traits)
        for ((key, value) in traits) {
            assertThat(copy).contains(MapEntry.entry(key, value))
        }
    }

    @Test
    fun copyIsImmutable() {
        val copy = traits.unmodifiableCopy()
        try {
            copy["foo"] = "bar"
            fail("Inserting into copy should throw UnsupportedOperationException")
        } catch (ignored: UnsupportedOperationException) {
        }
    }

    @Test
    fun address() {
        val address = Traits.Address()

        address.putCity("Vancouver")
        assertThat(address.city()).isEqualTo("Vancouver")

        address.putCountry("Canada")
        assertThat(address.country()).isEqualTo("Canada")

        address.putPostalCode("V6R 1J3")
        assertThat(address.postalCode()).isEqualTo("V6R 1J3")

        address.putState("BC")
        assertThat(address.state()).isEqualTo("BC")

        address.putStreet("128 W Hastings")
        assertThat(address.street()).isEqualTo("128 W Hastings")

        traits.putAddress(address)
        assertThat(traits.address()).isEqualTo(address)
    }

    @Test
    fun age() {
        traits.putAge(25)
        assertThat(traits.age()).isEqualTo(25)
    }

    @Test
    fun avatar() {
        traits.putAvatar("https://github.com/identicons/segmentio.png")
        assertThat(traits.avatar()).isEqualTo("https://github.com/identicons/segmentio.png")
    }

    @Test
    fun emptyBirthdayDoesNotCrash() {
        assertThat(traits.birthday()).isNull()
    }

    @Test
    fun birthday() {
        val date = GregorianCalendar(1992, 2, 10).time
        traits.putBirthday(date)
        assertThat(traits.birthday()).isEqualTo(date)
    }

    @Test
    fun createdAt() {
        traits.putCreatedAt("16-02-217")
        assertThat(traits.createdAt()).isEqualTo("16-02-217")
    }

    @Test
    fun description() {
        traits.putDescription("a really amazing library")
        assertThat(traits.description()).isEqualTo("a really amazing library")
    }

    @Test
    fun email() {
        traits.putEmail("prateek@gmail.com")
        assertThat(traits.email()).isEqualTo("prateek@gmail.com")
    }

    @Test
    fun employees() {
        traits.putEmployees(1000)
        assertThat(traits.employees()).isEqualTo(1000)
    }

    @Test
    fun fax() {
        traits.putFax("123-456-7890")
        assertThat(traits.fax()).isEqualTo("123-456-7890")
    }

    @Test
    fun gender() {
        traits.putGender("male")
        assertThat(traits.gender()).isEqualTo("male")
    }

    @Test
    fun industry() {
        traits.putIndustry("SAAS")
        assertThat(traits.industry()).isEqualTo("SAAS")
    }

    @Test
    fun name() {
        assertThat(traits.name()).isNull()

        traits.putFirstName("prateek")
        traits.putLastName("srivastava")
        assertThat(traits.name()).isEqualTo("prateek srivastava")

        traits.putName("mr. prateek srivastava")
        assertThat(traits.name()).isEqualTo("mr. prateek srivastava")
    }

    @Test
    fun firstName() {
        traits.putFirstName("prateek")
        assertThat(traits.name()).isEqualTo("prateek")
        assertThat(traits.firstName()).isEqualTo("prateek")
    }

    @Test
    fun lastName() {
        traits.putLastName("srivastava")
        assertThat(traits.name()).isEqualTo("srivastava")
        assertThat(traits.lastName()).isEqualTo("srivastava")
    }

    @Test
    fun phone() {
        traits.putPhone("123-456-7890")
        assertThat(traits.phone()).isEqualTo("123-456-7890")
    }

    @Test
    fun title() {
        traits.putTitle("Software Engineer")
        assertThat(traits.title()).isEqualTo("Software Engineer")
    }

    @Test
    fun username() {
        traits.putUsername("f2prateek")
        assertThat(traits.username()).isEqualTo("f2prateek")
    }

    @Test
    fun website() {
        traits.putWebsite("https://segment.com/")
        assertThat(traits.website()).isEqualTo("https://segment.com/")
    }
}
