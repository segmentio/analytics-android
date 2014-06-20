package com.segment.android.stats;

public class AnalyticsStatistics extends Statistics {

  private static final long serialVersionUID = 5469315718941515883L;

  private static String IDENTIFY_KEY = "Identify";
  private static String TRACK_KEY = "Track";
  private static String SCREEN_KEY = "Screen";
  private static String GROUP_KEY = "Group";
  private static String ALiAS_KEY = "Alias";

  private static String INSERT_ATTEMPTS_KEY = "Insert Attempts";
  private static String FLUSHED_ATTEMPTS_KEY = "Flushed Attempts";

  private static String INSERT_TIME_KEY = "Insert Time";
  private static String REQUEST_TIME_KEY = "Request Time";
  private static String FLUSH_TIME_KEY = "Flush Time";

  private static String SUCCESSFUL_KEY = "Successful";
  private static String FAILED_KEY = "Failed";

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
