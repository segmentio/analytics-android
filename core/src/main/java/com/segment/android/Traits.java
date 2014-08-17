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
import com.segment.android.internal.util.ISO8601Time;
import java.util.HashMap;
import java.util.Map;

import static com.segment.android.internal.util.Utils.getDeviceId;

/**
 * Traits can be anything you want, but some of them have semantic meaning and we treat them in
 * special ways. For example, whenever we see an email trait, we expect it to be the user's email
 * address. And we'll send this on to integrations that need an email, like Mailchimp. For that
 * reason, you should only use special traits for their intended purpose.
 * <p/>
 * This is persisted to disk, and will be remembered between sessions. todo: document API to clear
 * the user traits
 */
public class Traits {
  public static class Address {
    public String city;
    public String country;
    public String postalCode;
    public String state;
    public String street;

    Address(String city, String country, String postalCode, String state, String street) {
      this.city = city;
      this.country = country;
      this.postalCode = postalCode;
      this.state = state;
      this.street = street;
    }
  }

  public String avatar;
  public String createdAt;
  public String description;
  public String email;
  public String fax;
  public String id;
  public String name;
  public String phone;
  public String website;
  public Map<String, Object> other;

  // For Identify Calls
  public short age;
  public ISO8601Time birthday;
  public String firstName;
  public String gender;
  public String lastName;
  public String title;
  public String username;

  // For Group calls
  public long employees;
  public String industry;

  private Traits(Context context) {
    setId(getDeviceId(context));
    other = new HashMap<String, Object>();
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

  public Traits setAvatar(String avatar) {
    this.avatar = avatar;
    return this;
  }

  public Traits setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Traits setDescription(String description) {
    this.description = description;
    return this;
  }

  public Traits setEmail(String email) {
    this.email = email;
    return this;
  }

  public Traits setFax(String fax) {
    this.fax = fax;
    return this;
  }

  public Traits setId(String id) {
    this.id = id;
    return this;
  }

  public String getId() {
    return id;
  }

  public Traits setName(String name) {
    this.name = name;
    return this;
  }

  public Traits setPhone(String phone) {
    this.phone = phone;
    return this;
  }

  public Traits setWebsite(String website) {
    this.website = website;
    return this;
  }

  public Traits setAge(short age) {
    this.age = age;
    return this;
  }

  public Traits setBirthday(ISO8601Time birthday) {
    this.birthday = birthday;
    return this;
  }

  public Traits setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public Traits setGender(String gender) {
    this.gender = gender;
    return this;
  }

  public Traits setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public Traits setTitle(String title) {
    this.title = title;
    return this;
  }

  public Traits setUsername(String username) {
    this.username = username;
    return this;
  }

  public Traits setEmployees(long employees) {
    this.employees = employees;
    return this;
  }

  public Traits setIndustry(String industry) {
    this.industry = industry;
    return this;
  }

  public Traits put(String key, Object value) {
    other.put(key, value);
    return this;
  }

  public String getAvatar() {
    return avatar;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public String getDescription() {
    return description;
  }

  public String getEmail() {
    return email;
  }

  public String getFax() {
    return fax;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getWebsite() {
    return website;
  }

  public Map<String, Object> getOther() {
    return other;
  }

  public short getAge() {
    return age;
  }

  public ISO8601Time getBirthday() {
    return birthday;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getGender() {
    return gender;
  }

  public String getLastName() {
    return lastName;
  }

  public String getTitle() {
    return title;
  }

  public String getUsername() {
    return username;
  }

  public long getEmployees() {
    return employees;
  }

  public String getIndustry() {
    return industry;
  }
}
