/*
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

package com.segment.android;

import android.content.Context;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

import static com.segment.android.JsonUtils.jsonString;
import static com.segment.android.JsonUtils.toMap;

/**
 * Traits can be anything you want, but some of them have semantic meaning and we treat them in
 * special ways. For example, whenever we see an email trait, we expect it to be the user's email
 * address. And we'll send this on to integrations that need an email, like Mailchimp. For that
 * reason, you should only use special traits for their intended purpose.
 * <p/>
 * This is persisted to disk, and will be remembered between sessions.
 */
public class Traits {
  private static final String ADDRESS_KEY = "address";
  private static final String ADDRESS_CITY_KEY = "city";
  private static final String ADDRESS_COUNTRY_KEY = "country";
  private static final String ADDRESS_POSTAL_CODE_KEY = "postalCode";
  private static final String ADDRESS_STATE_KEY = "state";
  private static final String ADDRESS_STREET_KEY = "street";

  public Traits putAddress(String city, String country, String postalCode, String state,
      String street) {
    Map<String, String> address = new LinkedHashMap<String, String>(5);
    address.put(ADDRESS_CITY_KEY, city);
    address.put(ADDRESS_COUNTRY_KEY, country);
    address.put(ADDRESS_POSTAL_CODE_KEY, postalCode);
    address.put(ADDRESS_STATE_KEY, state);
    address.put(ADDRESS_STREET_KEY, street);
    return put(ADDRESS_KEY, address);
  }

  private static final String AVATAR_KEY = "avatar";
  private static final String CREATED_AT_KEY = "createdAt";
  private static final String DESCRIPTION_KEY = "description";
  private static final String EMAIL_KEY = "email";
  private static final String FAX_KEY = "fax";
  private static final String ID_KEY = "id";
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

  private final Map<String, Object> jsonMap;

  static Traits fromJson(String json) throws JSONException {
    JSONObject jsonObject = new JSONObject(json);
    Map<String, Object> map = toMap(jsonObject);
    return new Traits(map);
  }

  Traits() {
    this.jsonMap = new LinkedHashMap<String, Object>(5);
  }

  Traits(Map<String, Object> jsonMap) {
    this.jsonMap = jsonMap;
  }

  private Traits(Context context) {
    this();
    putId(Utils.getDeviceId(context));
  }

  static Traits singleton = null;

  public static Traits with(Context context) {
    if (singleton == null) {
      synchronized (Traits.class) {
        if (singleton == null) {
          singleton = new Traits(context);
        }
      }
    }
    return singleton;
  }

  public Traits putAvatar(String avatar) {
    return put(AVATAR_KEY, avatar);
  }

  public Traits putCreatedAt(String createdAt) {
    return put(CREATED_AT_KEY, createdAt);
  }

  public Traits putDescription(String description) {
    return put(DESCRIPTION_KEY, description);
  }

  public Traits putEmail(String email) {
    return put(EMAIL_KEY, email);
  }

  public Traits putFax(String fax) {
    return put(FAX_KEY, fax);
  }

  public Traits putId(String id) {
    return put(ID_KEY, id);
  }

  public String getId() {
    return (String) jsonMap.get(ID_KEY);
  }

  public Traits putName(String name) {
    return put(NAME_KEY, name);
  }

  public Traits putPhone(String phone) {
    return put(PHONE_KEY, phone);
  }

  public Traits putWebsite(String website) {
    return put(WEBSITE_KEY, website);
  }

  public Traits putAge(short age) {
    return put(AGE_KEY, age);
  }

  public Traits putBirthday(Date birthday) {
    return put(BIRTHDAY_KEY, ISO8601Time.from(birthday).toString());
  }

  public Traits putFirstName(String firstName) {
    return put(FIRST_NAME_KEY, firstName);
  }

  public Traits putGender(String gender) {
    return put(GENDER_KEY, gender);
  }

  public Traits putLastName(String lastName) {
    return put(LAST_NAME_KEY, lastName);
  }

  public Traits putTitle(String title) {
    return put(TITLE_KEY, title);
  }

  public Traits putUsername(String username) {
    return put(USERNAME_KEY, username);
  }

  public Traits putEmployees(long employees) {
    return put(EMPLOYEES_KEY, employees);
  }

  public Traits putIndustry(String industry) {
    return put(INDUSTRY_KEY, industry);
  }

  public Traits put(String key, Object value) {
    jsonMap.put(key, value);
    return this;
  }

  @Override public String toString() {
    return jsonString(jsonMap);
  }
}
