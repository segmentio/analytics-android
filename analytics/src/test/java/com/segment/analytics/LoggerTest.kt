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
package com.segment.analytics

import android.util.Log
import com.segment.analytics.integrations.Logger
import java.lang.Exception
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

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

        assertThat(ShadowLog.getLogs()).hasSize(4)
    }

    @Test
    fun verboseMessagesShowInLog() {
        val logger = Logger.with(Analytics.LogLevel.VERBOSE)
        logger.verbose("some message with an %s", "argument")
        assertThat(ShadowLog.getLogs())
            .containsExactly(
                LogItemBuilder()
                    .type(Log.VERBOSE)
                    .msg("some message with an argument")
                    .build()
            )
    }

    @Test
    fun debugMessagesShowInLog() {
        val logger = Logger.with(Analytics.LogLevel.DEBUG)

        logger.debug("some message with an %s", "argument")

        assertThat(ShadowLog.getLogs())
            .containsExactly(
                LogItemBuilder()
                    .type(Log.DEBUG)
                    .msg("some message with an argument")
                    .build()
            )
    }

    @Test
    fun infoMessagesShowInLog() {
        val logger = Logger.with(Analytics.LogLevel.INFO)

        logger.info("some message with an %s", "argument")

        assertThat(ShadowLog.getLogs())
            .containsExactly(
                LogItemBuilder()
                    .type(Log.INFO)
                    .msg("some message with an argument")
                    .build()
            )
    }

    @Test
    @Throws(Exception::class)
    fun errorMessagesShowInLog() {
        val logger = Logger.with(Analytics.LogLevel.DEBUG)

        val throwable = AssertionError("testing")
        logger.error(throwable, "some message with an %s", "argument")

        assertThat(ShadowLog.getLogs()).containsExactly(
            LogItemBuilder()
                .type(Log.ERROR)
                .throwable(throwable)
                .msg("some message with an argument")
                .build()
        )
    }

    @Test
    @Throws(Exception::class)
    fun subLog() {
        val logger = Logger.with(Analytics.LogLevel.DEBUG).subLog("foo")

        logger.debug("some message with an %s", "argument")

        assertThat(ShadowLog.getLogs()).containsExactly(
            LogItemBuilder()
                .tag("Analytics-foo")
                .type(Log.DEBUG)
                .msg("some message with an argument")
                .build()
        )
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

        fun tag(tag: String): LogItemBuilder {
            this.tag = tag
            return this
        }

        fun msg(msg: String): LogItemBuilder {
            this.msg = msg
            return this
        }

        fun throwable(throwable: Throwable): LogItemBuilder {
            this.throwable = throwable
            return this
        }

        fun build(): ShadowLog.LogItem {
            return ShadowLog.LogItem(type, tag, msg, throwable)
        }
    }
}
