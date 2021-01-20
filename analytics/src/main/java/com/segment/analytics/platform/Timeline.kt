package com.segment.analytics.platform

import com.segment.analytics.Analytics
import com.segment.analytics.integrations.BasePayload

// Platform abstraction for managing all plugins and their execution
// Currently the execution follows
//      Before -> Enrichment -> Destination -> After
internal class Timeline {
    private val plugins: Map<Plugin.Type, Mediator> = mapOf(
            Plugin.Type.Before to Mediator(mutableListOf()),
            Plugin.Type.Enrichment to Mediator(mutableListOf()),
            Plugin.Type.Destination to Mediator(mutableListOf()),
            Plugin.Type.After to Mediator(mutableListOf()),
            Plugin.Type.Utility to Mediator(mutableListOf())
    )
    lateinit var analytics: Analytics

    // initiate the event's lifecycle
    fun process(incomingEvent: BasePayload): BasePayload? {
        val beforeResult = applyPlugins(Plugin.Type.Before, incomingEvent)
        val enrichmentResult = applyPlugins(Plugin.Type.Enrichment, beforeResult)

        // once the event enters a destination, we don't want
        // to know about changes that happen there
        applyPlugins(Plugin.Type.Destination, enrichmentResult)

        // note: confused about why we pass the not fully enriched event here
        val afterResult = applyPlugins(Plugin.Type.After, enrichmentResult)

        print("System: ")
        if (afterResult == null) {
            print("event dropped.") // note: this doesnt make much sense, events should be in destination.
        }
        print(afterResult)
        return afterResult
    }

    // Applies a closure on all registered plugins
    fun applyClosure(closure: (Plugin) -> Unit) {
        plugins.forEach { (_, mediator) ->
            mediator.plugins.forEach {
                closure(it)
            }
        }
    }

    // Runs all registered plugins of a particular type on given payload
    fun applyPlugins(type: Plugin.Type, event: BasePayload?): BasePayload? {
        var result: BasePayload? = event
        val mediator = plugins[type]
        result = applyPlugins(mediator, result)
        return result
    }

    // Run a mediator on given payload
    fun applyPlugins(mediator: Mediator?, event: BasePayload?): BasePayload? {
        var result: BasePayload? = event
        result?.let { e ->
            result = mediator?.execute(e)
        }
        return result
    }

    // Register a new plugin
    fun add(plugin: Plugin) {
        plugins[plugin.type]?.add(plugin)
        plugin.setup(analytics)
    }

    // Remove a registered plugin
    fun remove(pluginName: String) {
        // remove all plugins with this name in every category
        plugins.forEach { (_, list) ->
            list.remove(pluginName)
        }
    }
}
