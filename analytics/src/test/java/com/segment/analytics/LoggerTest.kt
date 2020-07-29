  package com.segment.analytics

  import android.util.Log
  import com.segment.analytics.integrations.Logger
  import org.assertj.core.api.Assertions
  import org.junit.Test
  import org.junit.runner.RunWith
  import org.robolectric.RobolectricTestRunner
  import org.robolectric.annotation.Config
  import org.robolectric.shadows.ShadowLog
  import java.lang.Exception
  import kotlin.jvm.Throws

  @RunWith(RobolectricTestRunner::class)
  @Config(manifest = Config.NONE)
  class LoggerTest {
    @Test
    fun verboseLevelLogsEverything() {
      val logger = Logger.with(Analytics.LogLevel.VERBOSE)

      logger.debug("foo")
      logger.info("bar")
      logger.verbose("qaz")
      logger.error(null, "qux")

      Assertions.assertThat(ShadowLog.getLogs()).hasSize(4)
    }

    @Test
    fun verboseMessagesShowInLog() {
      val logger = Logger.with(Analytics.LogLevel.VERBOSE)
      logger.verbose("some message with an %s", "argument")
      Assertions.assertThat(ShadowLog.getLogs())
          .containsExactly(
              LogItemBuilder()
                  .type(Log.VERBOSE)
                  .msg("some message with an argument")
                  .build())
    }

    @Test
    fun debugMessagesShowInLog() {
      val logger = Logger.with(Analytics.LogLevel.DEBUG)

      logger.debug("some message with an %s", "argument")

      Assertions.assertThat(ShadowLog.getLogs())
          .containsExactly(
              LogItemBuilder()
                  .type(Log.DEBUG)
                  .msg("some message with an argument")
                  .build())
    }

    @Test
    fun infoMessagesShowInLog() {
      val logger = Logger.with(Analytics.LogLevel.INFO)

      logger.info("some message with an %s", "argument")

      Assertions.assertThat(ShadowLog.getLogs())
          .containsExactly(
              LogItemBuilder()
                  .type(Log.INFO)
                  .msg("some message with an argument")
                  .build())
    }

    @Test
    @Throws(Exception::class)
    fun errorMessagesShowInLog() {
      val logger = Logger.with(Analytics.LogLevel.DEBUG)

      val throwable = AssertionError("testing")
      logger.error(throwable, "some message with an %s", "argument")

      Assertions.assertThat(ShadowLog.getLogs()).containsExactly(
          LogItemBuilder()
              .type(Log.ERROR)
              .throwable(throwable)
              .msg("some message with an argument")
              .build())
    }

    @Test
    @Throws(Exception::class)
    fun subLog() {
      val logger = Logger.with(Analytics.LogLevel.DEBUG).subLog("foo")

      logger.debug("some message with an %s", "argument")

      Assertions.assertThat(ShadowLog.getLogs()).containsExactly(
          LogItemBuilder()
              .tag("Analytics-foo")
              .type(Log.DEBUG)
              .msg("some message with an argument")
              .build())
    }

    class LogItemBuilder {
      private var type: Int = 0
      private var tag: String = "Analytics"
      private var msg: String = ""
      private var throwable: Throwable? = null

      fun type(type: Int): LogItemBuilder {
        this.type = type
        return this
      }

      fun tag(tag: String): LogItemBuilder{
        this.tag = tag
        return this
      }

      fun msg(msg: String): LogItemBuilder{
        this.msg = msg
        return this
      }

      fun throwable(throwable: Throwable): LogItemBuilder{
        this.throwable = throwable
        return this
      }

      fun build(): ShadowLog.LogItem{
        return ShadowLog.LogItem(type, tag, msg, throwable)
      }
    }
  }