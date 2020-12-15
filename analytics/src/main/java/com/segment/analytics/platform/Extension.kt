package com.segment.analytics.platform

import com.segment.analytics.Analytics
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload

interface Extension {
    enum class Type {
        Before, Enrichment, Destination, After
    }

    val type: Type
    val name: String
    var analytics: Analytics?

    fun execute() {
        // empty body default
    }
}

interface EventExtension : Extension {
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

abstract class DestinationExtension : EventExtension {
    private val timeline: Timeline = Timeline()

    fun add(extension: Extension) {
        extension.analytics = this.analytics
        timeline.add(extension)
    }

    fun remove(extensionName: String) {
        timeline.remove(extensionName)
    }

    fun process(event: BasePayload?): BasePayload? {
        val beforeResult = timeline.applyExtensions(Extension.Type.Before, event)
        val enrichmentResult = timeline.applyExtensions(Extension.Type.Enrichment, beforeResult)

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

        val afterResult = timeline.applyExtensions(Extension.Type.After, enrichmentResult)

        println("Destination: $name")
        if (afterResult == null) {
            println("event dropped.")
        }
        return afterResult
    }

    open fun flush() {}

    open fun reset() {}
}