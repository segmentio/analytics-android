package com.segment.analytics

import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.Integration
import com.segment.analytics.integrations.Logger
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload
import com.segment.analytics.internal.Private
import java.util.concurrent.TimeUnit

internal class EventRunner(
        private val logger: Logger,
        private val sourceMiddleware: List<Middleware>,
        private val destinationMiddleware: Map<String, List<Middleware>>,
        private val stats: Stats
) {
    fun runSourceMiddleware(payload: BasePayload, integrations: Map<String, Integration<*>>, projectSettings: ProjectSettings) {
        logger.verbose("Created payload %s.", payload)
        val runner = MiddlewareRunner(sourceMiddleware)
        val nextPayload = runner.run(payload) ?: return
        runDestinationMiddleware(nextPayload, integrations, projectSettings)
    }

    private fun runDestinationMiddleware(payload: BasePayload, integrations: Map<String, Integration<*>>, projectSettings: ProjectSettings) {
        logger.verbose("Running payload %s.", payload)
        val operation = segmentEvent(payload, destinationMiddleware)
        Analytics.HANDLER.post { runIntegrationOperation(operation, integrations, projectSettings) }
    }

    fun runIntegrationOperation(operation: IntegrationOperation, integrations: Map<String, Integration<*>>, projectSettings: ProjectSettings) {
        for ((key, value) in integrations.entries) {
            val startTime = System.nanoTime()
            operation.run(key, value, projectSettings)
            val endTime = System.nanoTime()
            val durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)
            stats.dispatchIntegrationOperation(key, durationInMillis)
            logger.debug("Ran %s on integration %s in %d ns.", operation, key, endTime - startTime)
        }
    }
}

/**
 * Integration Operation for a Segment Event (track | identify | alias | group | screen)
 * Operation runs destination middleware for given integration before sending to the appropriate
 * integration
 */
@Private
internal fun segmentEvent(payload: BasePayload, destinationMiddleware: Map<String, List<Middleware>>): IntegrationOperation {
    return object : IntegrationOperation() {
        override fun run(key: String, integration: Integration<*>, projectSettings: ProjectSettings) {
            val applicableMiddleware = destinationMiddleware[key] ?: emptyList()
            val runner = MiddlewareRunner(applicableMiddleware)
            val nextPayload = runner.run(payload) ?: return

            when (nextPayload.type()) {
                BasePayload.Type.identify -> identify(nextPayload as IdentifyPayload, key, integration)
                BasePayload.Type.alias -> alias(nextPayload as AliasPayload, key, integration)
                BasePayload.Type.group -> group(nextPayload as GroupPayload, key, integration)
                BasePayload.Type.track -> track(nextPayload as TrackPayload, key, integration, projectSettings)
                BasePayload.Type.screen -> screen(nextPayload as ScreenPayload, key, integration)
            }
        }

        override fun toString(): String {
            return payload.toString()
        }
    }
}