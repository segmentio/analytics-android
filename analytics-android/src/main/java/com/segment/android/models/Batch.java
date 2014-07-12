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

package com.segment.android.models;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;

public class Batch extends EasyJSONObject {

  private final static String WRITE_KEY = "writeKey";
  private final static String BATCH_KEY = "batch";
  private final static String MESSAGE_ID_KEY = "messageId";
  private final static String SENT_AT_KEY = "sentAt";

  public Batch(String writeKey, List<BasePayload> batch) {
    setWriteKey(writeKey);
    setBatch(batch);
    setMessageId(UUID.randomUUID().toString());
  }

  public String getWriteKey() {
    return this.optString(WRITE_KEY, null);
  }

  public void setWriteKey(String writeKey) {
    this.put(WRITE_KEY, writeKey);
  }

  public List<BasePayload> getBatch() {
    return this.<BasePayload>getArray(BATCH_KEY);
  }

  public void setBatch(List<BasePayload> batch) {
    this.put(BATCH_KEY, new JSONArray(batch));
  }

  public Calendar getSentAt() {
    return getCalendar(SENT_AT_KEY);
  }

  public void setSentAt(Calendar sentAt) {
    super.put(SENT_AT_KEY, sentAt);
  }

  public String getMessageId() {
    return this.getString(MESSAGE_ID_KEY);
  }

  public void setMessageId(String messageId) {
    super.put(MESSAGE_ID_KEY, messageId);
  }
}