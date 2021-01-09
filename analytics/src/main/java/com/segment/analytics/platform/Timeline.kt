package com.segment.analytics.platform

import com.segment.analytics.integrations.BasePayload

// Platform abstraction for managing all extensions and their execution
// Currently the execution follows
//      Before -> Enrichment -> Destination -> After
internal class Timeline {
    private val plugins: Map<Extension.Type, Mediator> = mapOf(
            Extension.Type.Before to Mediator(mutableListOf()),
            Extension.Type.Enrichment to Mediator(mutableListOf()),
            Extension.Type.Destination to Mediator(mutableListOf()),
            Extension.Type.After to Mediator(mutableListOf()),
            Extension.Type.Utility to Mediator(mutableListOf())
    )

    // initiate the event's lifecycle
    fun process(incomingEvent: BasePayload): BasePayload? {
        val beforeResult = applyExtensions(Extension.Type.Before, incomingEvent)
        val enrichmentResult = applyExtensions(Extension.Type.Enrichment, beforeResult)

        // once the event enters a destination, we don't want
        // to know about changes that happen there
        applyExtensions(Extension.Type.Destination, enrichmentResult)

        // note: confused about why we pass the not fully enriched event here
        val afterResult = applyExtensions(Extension.Type.After, enrichmentResult)

        print("System: ")
        if (afterResult == null) {
            print("event dropped.")
        }
        print(afterResult)
        return afterResult
    }

    // Applies a closure on all registered extensions
    fun applyClosure(closure: (Extension) -> Unit) {
        plugins.forEach { (_, mediator) ->
            mediator.extensions.forEach {
                closure(it)
            }
        }
    }

    // Runs all registered extensions of a particular type on given payload
    fun applyExtensions(type: Extension.Type, event: BasePayload?): BasePayload? {
        var result: BasePayload? = event
        val mediator = plugins[type]
        result = applyExtensions(mediator, result)
        return result
    }

    // Run a mediator on given payload
    fun applyExtensions(mediator: Mediator?, event: BasePayload?): BasePayload? {
        var result: BasePayload? = event
        result?.let { e ->
            result = mediator?.execute(e)
        }
        return result
    }

    // Register a new extension
    fun add(extension: Extension) {
        plugins[extension.type]?.add(extension)
    }

    // Remove a registered extension
    fun remove(extensionName: String) {
        // remove all extensions with this name in every category
        plugins.forEach { (_, list) ->
            list.remove(extensionName)
        }
    }
}
