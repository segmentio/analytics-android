package com.segment.analytics.platform

import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload

// Platform abstraction for managing plugins' execution (of a specific type)
internal class Mediator(internal val plugins: MutableList<Plugin>) {

    fun add(plugin: Plugin) {
        plugins.add(plugin)
    }

    fun remove(pluginName: String) {
        plugins.removeAll { it.name == pluginName }
    }

    fun execute(event: BasePayload): BasePayload? {
        var result: BasePayload? = event

        plugins.forEach { plugin ->
            when (plugin) {
                is DestinationPlugin -> {
                    plugin.process(result)
                }
                is EventPlugin -> {
                    when (result) {
                        is IdentifyPayload -> {
                            result = plugin.identify(result as IdentifyPayload)
                        }
                        is TrackPayload -> {
                            result = plugin.track(result as TrackPayload)
                        }
                        is GroupPayload -> {
                            result = plugin.group(result as GroupPayload)
                        }
                        is ScreenPayload -> {
                            result = plugin.screen(result as ScreenPayload)
                        }
                        is AliasPayload -> {
                            result = plugin.alias(result as AliasPayload)
                        }
                    }
                }
                else -> {
                    plugin.execute()
                }
            }
        }
        return result
    }
}
