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

import static com.segment.analytics.internal.Utils.assertNotNullOrEmpty;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.segment.analytics.internal.Private;
import java.util.Date;
import java.util.Map;

public class AliasPayload extends BasePayload {

  static final String PREVIOUS_ID_KEY = "previousId";

  @Private
  AliasPayload(
      @NonNull String messageId,
      @NonNull Date timestamp,
      @NonNull Map<String, Object> context,
      @NonNull Map<String, Object> integrations,
      @Nullable String userId,
      @NonNull String anonymousId,
      @NonNull String previousId) {
    super(Type.alias, messageId, timestamp, context, integrations, userId, anonymousId);
    put(PREVIOUS_ID_KEY, previousId);
  }

  /**
   * The previous ID for the user that you want to alias from, that you previously called identify
   * with as their user ID, or the anonymous ID if you haven't identified the user yet.
   */
  public String previousId() {
    return getString(PREVIOUS_ID_KEY);
  }

  @Override
  public String toString() {
    return "AliasPayload{userId=\"" + userId() + ",previousId=\"" + previousId() + "\"}";
  }

  @NonNull
  @Override
  public AliasPayload.Builder toBuilder() {
    return new Builder(this);
  }

  /** Fluent API for creating {@link AliasPayload} instances. */
  public static final class Builder extends BasePayload.Builder<AliasPayload, Builder> {

    private String previousId;

    public Builder() {
      // Empty constructor.
    }

    @Private
    Builder(AliasPayload alias) {
      super(alias);
      this.previousId = alias.previousId();
    }

    @NonNull
    public Builder previousId(@NonNull String previousId) {
      this.previousId = assertNotNullOrEmpty(previousId, "previousId");
      return this;
    }

    @Override
    protected AliasPayload realBuild(
        @NonNull String messageId,
        @NonNull Date timestamp,
        @NonNull Map<String, Object> context,
        @NonNull Map<String, Object> integrations,
        @Nullable String userId,
        @NonNull String anonymousId) {
      assertNotNullOrEmpty(userId, "userId");
      assertNotNullOrEmpty(previousId, "previousId");

      return new AliasPayload(
          messageId, timestamp, context, integrations, userId, anonymousId, previousId);
    }

    @Override
    Builder self() {
      return this;
    }
  }
}
