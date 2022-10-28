package com.segment.analytics

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UnderwriterTest {
    lateinit var underwriter : Underwriter

    @Before
    fun setUp() {
        underwriter = Underwriter("test")
    }

    @Test
    @Throws(Exception::class)
    fun signatureMatches() {
        val part1 = "qwertyuiop[]\\"
        val part2 = "asdfghjkl;'"
        val part3 = "zxcvbnm,./"
        val whole = part1 + part2 + part3

        underwriter.update(whole.toByteArray())
        val signInOnce = underwriter.sign()
        underwriter.reset()

        underwriter.update(part1.toByteArray())
        underwriter.update(part2.toByteArray())
        underwriter.update(part3.toByteArray())
        val signMultipleTimes = underwriter.sign()
        underwriter.reset()

        Assert.assertEquals(signInOnce, signMultipleTimes)
    }
}