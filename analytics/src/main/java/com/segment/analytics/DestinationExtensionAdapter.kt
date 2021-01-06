package com.segment.analytics

import android.app.Activity
import android.os.Bundle
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.Integration
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload
import com.segment.analytics.internal.Private
import com.segment.analytics.platform.AndroidLifecycle
import com.segment.analytics.platform.DestinationExtension
import com.segment.analytics.platform.Extension

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

open class DestinationExtensionAdapter(private val key: String, private val integration: Integration<*>) : DestinationExtension(), AndroidLifecycle {

    override val type: Extension.Type = Extension.Type.Destination
    override val name: String = "destination-$key"
    override var analytics: Analytics? = null

    override fun track(payload: TrackPayload?): BasePayload? {
        payload?.let {
            segmentEvent(payload).run(key, integration, analytics?.projectSettings)
        }
        return null
    }

    override fun identify(payload: IdentifyPayload?): BasePayload? {
        payload?.let {
            segmentEvent(payload).run(key, integration, analytics?.projectSettings)
        }
        return null
    }

    override fun screen(payload: ScreenPayload?): BasePayload? {
        payload?.let {
            segmentEvent(payload).run(key, integration, analytics?.projectSettings)
        }
        return null
    }

    override fun group(payload: GroupPayload?): BasePayload? {
        payload?.let {
            segmentEvent(payload).run(key, integration, analytics?.projectSettings)
        }
        return null
    }

    override fun alias(payload: AliasPayload?): BasePayload? {
        payload?.let {
            segmentEvent(payload).run(key, integration, analytics?.projectSettings)
        }
        return null
    }

    override fun flush() {
        integration.flush()
    }

    override fun reset() {
        integration.reset()
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        integration.onActivityCreated(activity, savedInstanceState)
    }

    override fun onActivityStarted(activity: Activity?) {
        integration.onActivityStarted(activity)
    }

    override fun onActivityResumed(activity: Activity?) {
        integration.onActivityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity?) {
        integration.onActivityPaused(activity)
    }

    override fun onActivityStopped(activity: Activity?) {
        integration.onActivityStopped(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
        integration.onActivitySaveInstanceState(activity, outState)
    }

    override fun onActivityDestroyed(activity: Activity?) {
        integration.onActivityDestroyed(activity)
    }
}