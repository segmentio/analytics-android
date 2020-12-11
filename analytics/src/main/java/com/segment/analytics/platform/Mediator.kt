package com.segment.analytics.platform

import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload

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
                    result = extension.process(event)
                }
                is EventExtension -> {
                    when (event) {
                        is IdentifyPayload -> {
                            result = extension.identify(event)
                        }
                        is TrackPayload -> {
                            result = extension.track(event)
                        }
                        is GroupPayload -> {
                            result = extension.group(event)
                        }
                        is ScreenPayload -> {
                            result = extension.screen(event)
                        }
                        is AliasPayload -> {
                            result = extension.alias(event)
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
