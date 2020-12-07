package com.segment.analytics

import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.Integration
import com.segment.analytics.integrations.Logger
import java.util.concurrent.TimeUnit

internal class EventRunner(
        private val logger: Logger,
        private val sourceMiddleware: List<Middleware>,
        private val destinationMiddleware: Map<String, List<Middleware>>,
        private val stats: Stats
) {
    fun runSourceMiddleware(payload: BasePayload, integrations: Map<String, Integration<*>>, projectSettings: ProjectSettings) {
        logger.verbose("Created payload %s.", payload)
        val chain: Middleware.Chain = MiddlewareChainRunner(0, payload, sourceMiddleware) {
            runDestinationMiddleware(it, integrations, projectSettings)
        }
        chain.proceed(payload)
    }

    private fun runDestinationMiddleware(payload: BasePayload, integrations: Map<String, Integration<*>>, projectSettings: ProjectSettings) {
        logger.verbose("Running payload %s.", payload)
        val operation = IntegrationOperation.segmentEvent(payload, destinationMiddleware)
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