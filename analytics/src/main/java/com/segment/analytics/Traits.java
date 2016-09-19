/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment, Inc.
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

import android.content.Context;
import com.segment.analytics.internal.Private;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.segment.analytics.internal.Utils.NullableConcurrentHashMap;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.toISO8601Date;
import static java.util.Collections.unmodifiableMap;

/**
 * A class representing information about a user.
 * <p/>
 * Traits can be anything you want, but some of them have semantic meaning and we treat them in
 * special ways. For example, whenever we see an email trait, we expect it to be the user's email
 * address. And we'll send this on to integrations that need an email, like Mailchimp. For that
 * reason, you should only use special traits for their intended purpose.
 * <p/>
 * Traits are persisted to disk, and will be remembered between application and system reboots.
 */
public class Traits extends ValueMap {

  private static final String AVATAR_KEY = "avatar";
  private static final String CREATED_AT_KEY = "createdAt";
  private static final String DESCRIPTION_KEY = "description";
  private static final String EMAIL_KEY = "email";
  private static final String FAX_KEY = "fax";
  private static final String ANONYMOUS_ID_KEY = "anonymousId";
  private static final String USER_ID_KEY = "userId";
  private static final String NAME_KEY = "name";
  private static final String PHONE_KEY = "phone";
  private static final String WEBSITE_KEY = "website";
  // For Identify Calls
  private static final String AGE_KEY = "age";
  private static final String BIRTHDAY_KEY = "birthday";
  private static final String FIRST_NAME_KEY = "firstName";
  private static final String GENDER_KEY = "gender";
  private static final String LAST_NAME_KEY = "lastName";
  private static final String TITLE_KEY = "title";
  private static final String USERNAME_KEY = "username";
  // For Group calls
  private static final String EMPLOYEES_KEY = "employees";
  private static final String INDUSTRY_KEY = "industry";
  // Address
  private static final String ADDRESS_KEY = "address";

  /**
   * Create a new Traits instance with an anonymous ID. Analytics client can be called on any
   * thread, so this instance is thread safe.
   */
  static Traits create() {
    Traits traits = new Traits(new NullableConcurrentHashMap<String, Object>());
    traits.putAnonymousId(UUID.randomUUID().toString());
    return traits;
  }

  /** For deserialization from disk by {@link Traits.Cache}. */
  @Private Traits(Map<String, Object> delegate) {
    super(delegate);
  }

  // Public Constructor
  public Traits() {
  }

  public Traits(int initialCapacity) {
    super(initialCapacity);
  }

  public Traits unmodifiableCopy() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>(this);
    return new Traits(unmodifiableMap(map));
  }

  /**
   * Private API, users should call {@link com.segment.analytics.Analytics#identify(String)}
   * instead. Note that this is unable to enforce it, users can easily do traits.put(id, ..);
   */
  Traits putUserId(String id) {
    return putValue(USER_ID_KEY, id);
  }

  public String userId() {
    return getString(USER_ID_KEY);
  }

  Traits putAnonymousId(String id) {
    return putValue(ANONYMOUS_ID_KEY, id);
  }

  public String anonymousId() {
    return getString(ANONYMOUS_ID_KEY);
  }

  /**
   * Returns the currentId the user is identified with. This could be the user id or the
   * anonymous ID.
   */
  public String currentId() {
    String userId = userId();
    return (isNullOrEmpty(userId)) ? anonymousId() : userId;
  }

  /** Set an address for the user or group. */
  public Traits putAddress(Address address) {
    return putValue(ADDRESS_KEY, address);
  }

  public Address address() {
    return getValueMap(ADDRESS_KEY, Address.class);
  }

  /** Set the age of a user. */
  public Traits putAge(int age) {
    return putValue(AGE_KEY, age);
  }

  public int age() {
    return getInt(AGE_KEY, 0);
  }

  /** Set a URL to an avatar image for the user or group. */
  public Traits putAvatar(String avatar) {
    return putValue(AVATAR_KEY, avatar);
  }

  public String avatar() {
    return getString(AVATAR_KEY);
  }

  /** Set the user's birthday. */
  public Traits putBirthday(Date birthday) {
    return putValue(BIRTHDAY_KEY, toISO8601Date(birthday));
  }

  public Date birthday() {
    try {
      String birthday = getString(BIRTHDAY_KEY);
      if (isNullOrEmpty(birthday)) return null;
      return toISO8601Date(birthday);
    } catch (ParseException e) {
      return null;
    }
  }

  /**
   * Set the date the user’s or group’s account was first created. We accept date objects and a
   * wide range of date formats, including ISO strings and Unix timestamps. Feel free to use
   * whatever format is easiest for you - although ISO string is recommended for Android.
   */
  public Traits putCreatedAt(String createdAt) {
    return putValue(CREATED_AT_KEY, createdAt);
  }

  public String createdAt() {
    return getString(CREATED_AT_KEY);
  }

  /** Set a description of the user or group, like a personal bio. */
  public Traits putDescription(String description) {
    return putValue(DESCRIPTION_KEY, description);
  }

  public String description() {
    return getString(DESCRIPTION_KEY);
  }

  /** Set the email address of a user or group. */
  public Traits putEmail(String email) {
    return putValue(EMAIL_KEY, email);
  }

  public String email() {
    return getString(EMAIL_KEY);
  }

  /** Set the number of employees of a group, typically used for companies. */
  public Traits putEmployees(long employees) {
    return putValue(EMPLOYEES_KEY, employees);
  }

  public long employees() {
    return getLong(EMPLOYEES_KEY, 0);
  }

  /** Set the fax number of a user or group. */
  public Traits putFax(String fax) {
    return putValue(FAX_KEY, fax);
  }

  public String fax() {
    return getString(FAX_KEY); // todo: maybe remove this, I doubt any bundled integration uses fax
  }

  /** Set the first name of a user. */
  public Traits putFirstName(String firstName) {
    return putValue(FIRST_NAME_KEY, firstName);
  }

  public String firstName() {
    return getString(FIRST_NAME_KEY);
  }

  /** Set the gender of a user. */
  public Traits putGender(String gender) {
    return putValue(GENDER_KEY, gender);
  }

  public String gender() {
    return getString(GENDER_KEY);
  }

  /**
   * Set the industry the user works in, or a group is part of.
   */
  public Traits putIndustry(String industry) {
    return putValue(INDUSTRY_KEY, industry);
  }

  public String industry() {
    return getString(INDUSTRY_KEY);
  }

  /** Set the last name of a user. */
  public Traits putLastName(String lastName) {
    return putValue(LAST_NAME_KEY, lastName);
  }

  public String lastName() {
    return getString(LAST_NAME_KEY);
  }

  /** Set the name of a user or group. */
  public Traits putName(String name) {
    return putValue(NAME_KEY, name);
  }

  public String name() {
    String name = getString(NAME_KEY);
    if (isNullOrEmpty(name) && isNullOrEmpty(firstName()) && isNullOrEmpty(lastName())) {
      return null;
    }

    if (isNullOrEmpty(name)) {
      StringBuilder stringBuilder = new StringBuilder();
      String firstName = firstName();
      boolean appendSpace = false;
      if (!isNullOrEmpty(firstName)) {
        appendSpace = true;
        stringBuilder.append(firstName);
      }

      String lastName = lastName();
      if (!isNullOrEmpty(lastName)) {
        if (appendSpace) stringBuilder.append(' ');
        stringBuilder.append(lastName);
      }
      return stringBuilder.toString();
    } else {
      return name;
    }
  }

  /** Set the phone number of a user or group. */
  public Traits putPhone(String phone) {
    return putValue(PHONE_KEY, phone);
  }

  public String phone() {
    return getString(PHONE_KEY);
  }

  /**
   * Set the title of a user, usually related to their position at a specific company, for example
   * "VP of Engineering"
   */
  public Traits putTitle(String title) {
    return putValue(TITLE_KEY, title);
  }

  public String title() {
    return getString(TITLE_KEY);
  }

  /**
   * Set the user’s username. This should be unique to each user, like the usernames of Twitter or
   * GitHub.
   */
  public Traits putUsername(String username) {
    return putValue(USERNAME_KEY, username);
  }

  public String username() {
    return getString(USERNAME_KEY);
  }

  /** Set the website of a user or group. */
  public Traits putWebsite(String website) {
    return putValue(WEBSITE_KEY, website);
  }

  public String website() {
    return getString(WEBSITE_KEY);
  }

  @Override public Traits putValue(String key, Object value) {
    super.putValue(key, value);
    return this;
  }

  /** Represents information about the address of a user or group. */
  public static class Address extends ValueMap {

    private static final String ADDRESS_CITY_KEY = "city";
    private static final String ADDRESS_COUNTRY_KEY = "country";
    private static final String ADDRESS_POSTAL_CODE_KEY = "postalCode";
    private static final String ADDRESS_STATE_KEY = "state";
    private static final String ADDRESS_STREET_KEY = "street";

    // Public constructor
    public Address() {
    }

    // For deserialization
    public Address(Map<String, Object> map) {
      super(map);
    }

    @Override public Address putValue(String key, Object value) {
      super.putValue(key, value);
      return this;
    }

    public Address putCity(String city) {
      return putValue(ADDRESS_CITY_KEY, city);
    }

    public String city() {
      return getString(ADDRESS_CITY_KEY);
    }

    public Address putCountry(String country) {
      return putValue(ADDRESS_COUNTRY_KEY, country);
    }

    public String country() {
      return getString(ADDRESS_COUNTRY_KEY);
    }

    public Address putPostalCode(String postalCode) {
      return putValue(ADDRESS_POSTAL_CODE_KEY, postalCode);
    }

    public String postalCode() {
      return getString(ADDRESS_POSTAL_CODE_KEY);
    }

    public Address putState(String state) {
      return putValue(ADDRESS_STATE_KEY, state);
    }

    public String state() {
      return getString(ADDRESS_STATE_KEY);
    }

    public Address putStreet(String street) {
      return putValue(ADDRESS_STREET_KEY, street);
    }

    public String street() {
      return getString(ADDRESS_STREET_KEY);
    }
  }

  static class Cache extends ValueMap.Cache<Traits> {

    // todo: remove. This is legacy behaviour from before we started namespacing the entire shared
    // preferences object and were namespacing keys instead.
    private static final String TRAITS_CACHE_PREFIX = "traits-";

    Cache(Context context, Cartographer cartographer, String tag) {
      super(context, cartographer, TRAITS_CACHE_PREFIX + tag, tag, Traits.class);
    }

    @Override public Traits create(Map<String, Object> map) {
      // Analytics client can be called on any thread, so this instance should be thread safe.
      return new Traits(new NullableConcurrentHashMap<>(map));
    }
  }
}
