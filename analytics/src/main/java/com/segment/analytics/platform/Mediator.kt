package com.segment.analytics.platform

import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload

// Platform abstraction for managing extensions' execution (of a specific type)
internal class Mediator(internal val extensions: MutableList<Extension>) {

    fun add(extension: Extension) {
        extensions.add(extension)
    }

    fun remove(extensionName: String) {
        extensions.removeAll { it.name == extensionName }
    }

    fun execute(event: BasePayload): BasePayload? {
        var result: BasePayload? = event

        extensions.forEach { extension ->
            when (extension) {
                is DestinationExtension -> {
                    extension.process(result)
                }
                is EventExtension -> {
                    when (result) {
                        is IdentifyPayload -> {
                            result = extension.identify(result as IdentifyPayload)
                        }
                        is TrackPayload -> {
                            result = extension.track(result as TrackPayload)
                        }
                        is GroupPayload -> {
                            result = extension.group(result as GroupPayload)
                        }
                        is ScreenPayload -> {
                            result = extension.screen(result as ScreenPayload)
                        }
                        is AliasPayload -> {
                            result = extension.alias(result as AliasPayload)
                        }
                    }
                }
                else -> {
                    extension.execute()
                }
            }
        }
        return result
    }
}
