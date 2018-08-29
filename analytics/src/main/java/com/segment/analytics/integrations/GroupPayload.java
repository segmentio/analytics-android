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
package com.segment.analytics.integrations;

import static com.segment.analytics.internal.Utils.assertNotNull;
import static com.segment.analytics.internal.Utils.assertNotNullOrEmpty;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.segment.analytics.Traits;
import com.segment.analytics.internal.Private;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class GroupPayload extends BasePayload {

  static final String GROUP_ID_KEY = "groupId";
  static final String TRAITS_KEY = "traits";

  @Private
  public GroupPayload(
      @NonNull String messageId,
      @NonNull Date timestamp,
      @NonNull Map<String, Object> context,
      @NonNull Map<String, Object> integrations,
      @Nullable String userId,
      @NonNull String anonymousId,
      @NonNull String groupId,
      @NonNull Map<String, Object> traits) {
    super(Type.group, messageId, timestamp, context, integrations, userId, anonymousId);
    put(GROUP_ID_KEY, groupId);
    put(TRAITS_KEY, traits);
  }

  /**
   * A unique identifier that refers to the group in your database. For example, if your product
   * groups people by "organization" you would use the organization's ID in your database as the
   * group ID.
   */
  @NonNull
  public String groupId() {
    return getString(GROUP_ID_KEY);
  }

  /** The group method also takes a traits dictionary, just like identify. */
  @NonNull
  public Traits traits() {
    return getValueMap(TRAITS_KEY, Traits.class);
  }

  @Override
  public String toString() {
    return "GroupPayload{groupId=\"" + groupId() + "\"}";
  }

  @NonNull
  @Override
  public GroupPayload.Builder toBuilder() {
    return new Builder(this);
  }

  /** Fluent API for creating {@link GroupPayload} instances. */
  public static class Builder extends BasePayload.Builder<GroupPayload, Builder> {

    private String groupId;
    private Map<String, Object> traits;

    public Builder() {
      // Empty constructor.
    }

    @Private
    Builder(GroupPayload group) {
      super(group);
      groupId = group.groupId();
      traits = group.traits();
    }

    @NonNull
    public Builder groupId(@NonNull String groupId) {
      this.groupId = assertNotNullOrEmpty(groupId, "groupId");
      return this;
    }

    @NonNull
    public Builder traits(@NonNull Map<String, ?> traits) {
      assertNotNull(traits, "traits");
      this.traits = Collections.unmodifiableMap(new LinkedHashMap<>(traits));
      return this;
    }

    @Override
    protected GroupPayload realBuild(
        @NonNull String messageId,
        @NonNull Date timestamp,
        @NonNull Map<String, Object> context,
        @NonNull Map<String, Object> integrations,
        @Nullable String userId,
        @NonNull String anonymousId) {
      assertNotNullOrEmpty(groupId, "groupId");

      Map<String, Object> traits = this.traits;
      if (isNullOrEmpty(traits)) {
        traits = Collections.emptyMap();
      }

      return new GroupPayload(
          messageId, timestamp, context, integrations, userId, anonymousId, groupId, traits);
    }

    @Override
    Builder self() {
      return this;
    }
  }
}
