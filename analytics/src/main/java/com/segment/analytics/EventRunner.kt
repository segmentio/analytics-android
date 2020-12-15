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

/**
 * Integration Operation for a Segment Event (track | identify | alias | group | screen)
 * Operation runs destination middleware for given integration before sending to the appropriate
 * integration
 */
@Private
internal fun segmentEvent(payload: BasePayload): IntegrationOperation {
    return object : IntegrationOperation() {
        override fun run(key: String, integration: Integration<*>, projectSettings: ProjectSettings) {
            when (payload.type()) {
                BasePayload.Type.identify -> identify(payload as IdentifyPayload, key, integration)
                BasePayload.Type.alias -> alias(payload as AliasPayload, key, integration)
                BasePayload.Type.group -> group(payload as GroupPayload, key, integration)
                BasePayload.Type.track -> track(payload as TrackPayload, key, integration, projectSettings)
                BasePayload.Type.screen -> screen(payload as ScreenPayload, key, integration)
            }
        }

        override fun toString(): String {
            return payload.toString()
        }
    }
}