package com.segment.analytics.platform

import com.segment.analytics.Analytics
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload

// Most simple interface for an plugin
interface Plugin {
    enum class Type {
        Before, // Executed before event processing begins.
        Enrichment, // Executed as the first level of event processing.
        Destination, // Executed as events begin to pass off to destinations.
        After, // Executed after all event processing is completed.  This can be used to perform cleanup operations, etc.
        Utility // Executed only when called manually, such as Logging.
    }

    val type: Type
    val name: String
    var analytics: Analytics?

    // A simple setup function thats executed when plugin is attached to analytics
    // If overriden, ensure that super.setup() is invoked
    fun setup(analytics: Analytics) {
        this.analytics = analytics
    }

    fun execute() {
        // empty body default
    }
}

// Advanced plugin that can act on specific event payloads
interface EventPlugin : Plugin {
    fun track(payload: TrackPayload?): BasePayload? {
        return payload
    }

    fun identify(payload: IdentifyPayload?): BasePayload? {
        return payload
    }

    fun screen(payload: ScreenPayload?): BasePayload? {
        return payload
    }

    fun group(payload: GroupPayload?): BasePayload? {
        return payload
    }

    fun alias(payload: AliasPayload?): BasePayload? {
        return payload
    }
}

// Basic interface for device-mode destinations. Allows overriding track, identify, screen, group, alias, flush and reset
abstract class DestinationPlugin : EventPlugin {
    override val type: Plugin.Type = Plugin.Type.Destination
    private val timeline: Timeline = Timeline()
    override var analytics: Analytics? = null

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        timeline.analytics = analytics
    }

    fun add(plugin: Plugin) {
        plugin.analytics = this.analytics
        timeline.add(plugin)
    }

    fun remove(pluginName: String) {
        timeline.remove(pluginName)
    }

    // Special function for DestinationPlugin that manages its own timeline execution
    fun process(event: BasePayload?): BasePayload? {
        val beforeResult = timeline.applyPlugins(Plugin.Type.Before, event)
        val enrichmentResult = timeline.applyPlugins(Plugin.Type.Enrichment, beforeResult)

        enrichmentResult?.let {
            when (it) {
                is IdentifyPayload -> {
                    identify(it)
                }
                is TrackPayload -> {
                    track(it)
                }
                is GroupPayload -> {
                    group(it)
                }
                is ScreenPayload -> {
                    screen(it)
                }
                is AliasPayload -> {
                    alias(it)
                }
                else -> {
                    null
                }
            }
        }

        val afterResult = timeline.applyPlugins(Plugin.Type.After, enrichmentResult)

        println("Destination: $name")
        if (afterResult == null) {
            println("event dropped.")
        }
        return afterResult
    }

    open fun flush() {}

    open fun reset() {}
}