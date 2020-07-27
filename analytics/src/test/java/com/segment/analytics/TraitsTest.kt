package com.segment.analytics

import org.assertj.core.api.Assertions
import org.assertj.core.data.MapEntry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TraitsTest {
    lateinit var traits: Traits

    @Before
    fun setUp() {
        traits = Traits.create()
    }

    @Test
    @Throws
    fun newInvocationHasUniqueId() {
        Assertions.assertThat(traits).isNotSameAs(Traits.create())
    }

    @Test
    @Throws
    fun newInvocationHasNoUserId() {
        Assertions.assertThat(traits.userId()).isNull()
    }

    @Test
    @Throws
    fun publicConstructorGivesEmptyTraits() {
        Assertions.assertThat(Traits()).hasSize(0)
    }

    @Test
    @Throws
    fun userIdOrAnonymousId() {
        Assertions.assertThat(Traits().putUserId("foo").putAnonymousId("bar").currentId())
                .isEqualTo("foo")
        Assertions.assertThat(Traits().putUserId("foo").currentId()).isEqualTo("foo")
        Assertions.assertThat(Traits().putAnonymousId("bar").currentId()).isEqualTo("bar")
        Assertions.assertThat(Traits().currentId()).isNull()
    }

    @Test
    fun traitsAreMergedCorrectly() {
        val traits1 = Traits()
                .putAge(20)
                .putAvatar("f2prateek")
                .putDescription("the first one")
                .putLastName("Srivastava")
                .putEmail("f2prateek@gmail.com")
                .putEmployees(50)
        Assertions.assertThat(traits1).hasSize(6)

        val traits2 = Traits()
                .putAvatar("f2prateek")
                .putFirstName("Prateek")
                .putDescription("the second one")
        Assertions.assertThat(traits2).hasSize(3)

        traits1.putAll(traits2)
        Assertions.assertThat(traits1)
                .hasSize(7)
                .contains(MapEntry.entry("avatar", "f2prateek"))
                .contains(MapEntry.entry("description", "the second one"))
                .contains(MapEntry.entry("email", "f2prateek@gmail.com"))
        Assertions.assertThat(traits1.name()).isEqualTo("Prateek Srivastava")
    }

    @Test
    fun copyReturnsSameMappings() {
        val copy = traits.unmodifiableCopy()
        Assertions.assertThat(copy).hasSameSizeAs(traits).isNotSameAs(traits).isEqualTo(traits)
        for ((key, value) in traits) {
            Assertions.assertThat(copy).contains(MapEntry.entry(key, value))
        }
    }

    @Test
    fun copyIsImmutable() {
        val copy = traits.unmodifiableCopy()
        try{
            copy["foo"] = "bar"
            Assertions.fail("Inserting into copy should throw UnsupportedOperationException")
        } catch (ignored: UnsupportedOperationException) {
        }
    }

    @Test
    fun address() {
        val address = Traits.Address()

        address.putCity("Vancouver")
        Assertions.assertThat(address.city()).isEqualTo("Vancouver")

        address.putCountry("Canada")
        Assertions.assertThat(address.country()).isEqualTo("Canada")

        address.putPostalCode("V6R 1J3")
        Assertions.assertThat(address.postalCode()).isEqualTo("V6R 1J3")

        address.putState("BC")
        Assertions.assertThat(address.state()).isEqualTo("BC")

        address.putStreet("128 W Hastings")
        Assertions.assertThat(address.street()).isEqualTo("128 W Hastings")

        traits.putAddress(address)
        Assertions.assertThat(traits.address()).isEqualTo(address)
    }

    @Test
    fun age() {
        traits.putAge(25)
        Assertions.assertThat(traits.age()).isEqualTo(25)
    }

    @Test
    fun avatar() {
        traits.putAvatar("https://github.com/identicons/segmentio.png")
        Assertions.assertThat(traits.avatar()).isEqualTo("https://github.com/identicons/segmentio.png")
    }

    @Test
    fun emptyBirthdayDoesNotCrash() {
        Assertions.assertThat(traits.birthday()).isNull()
    }

    @Test
    fun birthday() {
        val date = GregorianCalendar(1992, 2, 10).time
        traits.putBirthday(date)
        Assertions.assertThat(traits.birthday()).isEqualTo(date)
    }

    @Test
    fun createdAt() {
        traits.putCreatedAt("16-02-217")
        Assertions.assertThat(traits.createdAt()).isEqualTo("16-02-217")
    }

    @Test
    fun description() {
        traits.putDescription("a really amazing library")
        Assertions.assertThat(traits.description()).isEqualTo("a really amazing library")
    }

    @Test
    fun email() {
        traits.putEmail("prateek@gmail.com")
        Assertions.assertThat(traits.email()).isEqualTo("prateek@gmail.com")
    }

    @Test
    fun employees() {
        traits.putEmployees(1000)
        Assertions.assertThat(traits.employees()).isEqualTo(1000)
    }

    @Test
    fun fax() {
        traits.putFax("123-456-7890")
        Assertions.assertThat(traits.fax()).isEqualTo("123-456-7890")
    }

    @Test
    fun gender() {
        traits.putGender("male")
        Assertions.assertThat(traits.gender()).isEqualTo("male")
    }

    @Test
    fun industry() {
        traits.putIndustry("SAAS")
        Assertions.assertThat(traits.industry()).isEqualTo("SAAS")
    }

    @Test
    fun name() {
        Assertions.assertThat(traits.name()).isNull()

        traits.putFirstName("prateek")
        traits.putLastName("srivastava")
        Assertions.assertThat(traits.name()).isEqualTo("prateek srivastava")

        traits.putName("mr. prateek srivastava")
        Assertions.assertThat(traits.name()).isEqualTo("mr. prateek srivastava")
    }

    @Test
    fun firstName() {
        traits.putFirstName("prateek")
        Assertions.assertThat(traits.name()).isEqualTo("prateek")
        Assertions.assertThat(traits.firstName()).isEqualTo("prateek")
    }

    @Test
    fun lastName() {
        traits.putLastName("srivastava")
        Assertions.assertThat(traits.name()).isEqualTo("srivastava")
        Assertions.assertThat(traits.lastName()).isEqualTo("srivastava")
    }

    @Test
    fun phone() {
        traits.putPhone("123-456-7890")
        Assertions.assertThat(traits.phone()).isEqualTo("123-456-7890")
    }

    @Test
    fun title() {
        traits.putTitle("Software Engineer")
        Assertions.assertThat(traits.title()).isEqualTo("Software Engineer")
    }

    @Test
    fun username() {
        traits.putUsername("f2prateek")
        Assertions.assertThat(traits.username()).isEqualTo("f2prateek")
    }

    @Test
    fun website() {
        traits.putWebsite("https://segment.com/")
        Assertions.assertThat(traits.website()).isEqualTo("https://segment.com/")
    }
}