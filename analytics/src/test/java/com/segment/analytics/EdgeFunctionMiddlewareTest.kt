package com.segment.analytics

import android.Manifest
import com.google.common.util.concurrent.MoreExecutors
import com.segment.analytics.integrations.Integration
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.AssertionError
import java.lang.IllegalStateException
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EdgeFunctionMiddlewareTest {

    lateinit var builder: Analytics.Builder

    @Mock
    lateinit var integrationFoo: Integration<Void>

    @Mock
    lateinit var integrationBar: Integration<Void>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Analytics.INSTANCES.clear()
        TestUtils.grantPermission(RuntimeEnvironment.application, Manifest.permission.INTERNET)
        val projectSettings =
                ValueMap()
                        .putValue(
                                "integrations",
                                ValueMap()
                                        .putValue("foo", ValueMap().putValue("appToken", "foo_token"))
                                        .putValue(
                                                "bar",
                                                ValueMap()
                                                        .putValue("appToken", "foo_token")
                                                        .putValue("trackAttributionData", true)))
        builder =
                Analytics.Builder(RuntimeEnvironment.application, "write_key")
                        .defaultProjectSettings(projectSettings)
                        .use(
                                object : Integration.Factory {
                                    override fun create(settings: ValueMap, analytics: Analytics): Integration<*>? {
                                        return integrationFoo
                                    }

                                    override fun key(): String {
                                        return "foo"
                                    }
                                })
                        .use(
                                object : Integration.Factory {
                                    override fun create(settings: ValueMap, analytics: Analytics): Integration<*>? {
                                        return integrationBar
                                    }

                                    override fun key(): String {
                                        return "bar"
                                    }
                                })
                        .executor(MoreExecutors.newDirectExecutorService())
    }

    /** Edge Function Middleware Tests **/
    @Test
    @Throws(Exception::class)
    fun edgeFunctionMiddlewareCanExist() {

        val analytics = builder
                .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
                .build()

        analytics.track("foo")

        val privateEdgeFunctions: Field = Analytics::class.java.getDeclaredField("edgeFunctionMiddleware")
        Assertions.assertThat(privateEdgeFunctions).isNotNull
    }

    @Test
    @Throws(Exception::class)
    fun edgeFunctionMiddlewareOverwrites() {

        try {
            val analytics = builder
                    .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
                    .useSourceMiddleware(
                            Middleware { throw AssertionError("should not be invoked") })
                    .build()

            Assertions.fail("Should not reach this state")
        } catch (exception: java.lang.Exception) {
            Assertions.assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    @Throws(Exception::class)
    fun edgeFunctionDestinationMiddlewareOverwrites() {

        try {
            val analytics = builder
                    .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
                    .useDestinationMiddleware("test",
                            Middleware { throw AssertionError("should not be invoked") })
                    .build()

            Assertions.fail("Should not reach this state")
        } catch (exception: java.lang.Exception) {
            Assertions.assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    @Throws(Exception::class)
    fun edgeFunctionBothMiddlewareOverwrites() {

        try {
            val analytics = builder
                    .useEdgeFunctionMiddleware(Mockito.mock(JSMiddleware::class.java))
                    .useDestinationMiddleware("test",
                            Middleware { throw AssertionError("should not be invoked") })
                    .useSourceMiddleware(
                            Middleware { throw AssertionError("should not be invoked") })
                    .build()

            Assertions.fail("Should not reach this state")
        } catch (exception: java.lang.Exception) {
            Assertions.assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        }
    }
}