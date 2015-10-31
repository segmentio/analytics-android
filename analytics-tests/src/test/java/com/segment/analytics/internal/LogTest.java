package com.segment.analytics.internal;

import com.segment.analytics.Analytics;
import com.segment.analytics.core.tests.BuildConfig;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class LogTest {

  @Test public void verboseLevelLogsEverything() {
    Log log = Log.with(Analytics.LogLevel.VERBOSE);

    log.debug("foo");
    log.info("bar");
    log.verbose("qaz");
    log.error(null, "qux");

    assertThat(ShadowLog.getLogs()).hasSize(4);
  }

  @Test public void verboseMessagesShowInLog() {
    Log log = Log.with(Analytics.LogLevel.VERBOSE);

    log.verbose("some message with an %s", "argument");

    assertThat(ShadowLog.getLogs()) //
        .containsExactly(new LogItemBuilder() //
            .type(android.util.Log.VERBOSE) //
            .msg("some message with an argument") //
            .build());
  }

  @Test public void debugMessagesShowInLog() {
    Log log = Log.with(Analytics.LogLevel.DEBUG);

    log.debug("some message with an %s", "argument");

    assertThat(ShadowLog.getLogs()) //
        .containsExactly(new LogItemBuilder() //
            .type(android.util.Log.DEBUG) //
            .msg("some message with an argument") //
            .build());
  }

  @Test public void infoMessagesShowInLog() {
    Log log = Log.with(Analytics.LogLevel.INFO);

    log.info("some message with an %s", "argument");

    assertThat(ShadowLog.getLogs()) //
        .containsExactly(new LogItemBuilder() //
            .type(android.util.Log.INFO) //
            .msg("some message with an argument") //
            .build());
  }

  @Test public void errorMessagesShowInLog() throws Exception {
    Log log = Log.with(Analytics.LogLevel.DEBUG);
    Throwable throwable = new AssertionError("testing");
    log.error(throwable, "some message with an %s", "argument");

    assertThat(ShadowLog.getLogs()) //
        .containsExactly(new LogItemBuilder() //
            .type(android.util.Log.ERROR) //
            .throwable(throwable) //
            .msg("some message with an argument") //
            .build());
  }

  static class LogItemBuilder {

    private int type;
    private String tag = "Analytics"; // will be the default tag unless explicitly overridden
    private String msg;
    private Throwable throwable;

    public LogItemBuilder type(int type) {
      this.type = type;
      return this;
    }

    public LogItemBuilder tag(String tag) {
      this.tag = tag;
      return this;
    }

    public LogItemBuilder msg(String msg) {
      this.msg = msg;
      return this;
    }

    public LogItemBuilder throwable(Throwable throwable) {
      this.throwable = throwable;
      return this;
    }

    public ShadowLog.LogItem build() {
      return new ShadowLog.LogItem(type, tag, msg, throwable);
    }
  }
}
