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

package com.segment.android.stats;

public class AnalyticsStatistics extends Statistics {

  private static final long serialVersionUID = 5469315718941515883L;

  private static final String IDENTIFY_KEY = "Identify";
  private static final String TRACK_KEY = "Track";
  private static final String SCREEN_KEY = "Screen";
  private static final String GROUP_KEY = "Group";
  private static final String ALiAS_KEY = "Alias";

  private static final String INSERT_ATTEMPTS_KEY = "Insert Attempts";
  private static final String FLUSHED_ATTEMPTS_KEY = "Flushed Attempts";

  private static final String INSERT_TIME_KEY = "Insert Time";
  private static final String REQUEST_TIME_KEY = "Request Time";
  private static final String FLUSH_TIME_KEY = "Flush Time";

  private static final String SUCCESSFUL_KEY = "Successful";
  private static final String FAILED_KEY = "Failed";

  public Statistic getIdentifies() {
    return ensure(IDENTIFY_KEY);
  }

  public void updateIdentifies(double val) {
    update(IDENTIFY_KEY, val);
  }

  public Statistic getTracks() {
    return ensure(TRACK_KEY);
  }

  public void updateTracks(double val) {
    update(TRACK_KEY, val);
  }

  public Statistic getScreens() {
    return ensure(SCREEN_KEY);
  }

  public void updateScreens(double val) {
    update(SCREEN_KEY, val);
  }

  public Statistic getAlias() {
    return ensure(ALiAS_KEY);
  }

  public void updateAlias(double val) {
    update(ALiAS_KEY, val);
  }

  public Statistic getGroups() {
    return ensure(GROUP_KEY);
  }

  public void updateGroups(double val) {
    update(GROUP_KEY, val);
  }

  public Statistic getInsertAttempts() {
    return ensure(INSERT_ATTEMPTS_KEY);
  }

  public void updateInsertAttempts(double val) {
    update(INSERT_ATTEMPTS_KEY, val);
  }

  public Statistic getFlushAttempts() {
    return ensure(FLUSHED_ATTEMPTS_KEY);
  }

  public void updateFlushAttempts(double val) {
    update(FLUSHED_ATTEMPTS_KEY, val);
  }

  public Statistic getInsertTime() {
    return ensure(INSERT_TIME_KEY);
  }

  public void updateInsertTime(double val) {
    update(INSERT_TIME_KEY, val);
  }

  public Statistic getFlushTime() {
    return ensure(FLUSH_TIME_KEY);
  }

  public void updateFlushTime(double val) {
    update(FLUSH_TIME_KEY, val);
  }

  public Statistic getRequestTime() {
    return ensure(REQUEST_TIME_KEY);
  }

  public void updateRequestTime(double val) {
    update(REQUEST_TIME_KEY, val);
  }

  public Statistic getSuccessful() {
    return ensure(SUCCESSFUL_KEY);
  }

  public void updateSuccessful(double val) {
    update(SUCCESSFUL_KEY, val);
  }

  public Statistic getFailed() {
    return ensure(FAILED_KEY);
  }

  public void updateFailed(double val) {
    update(FAILED_KEY, val);
  }
}
