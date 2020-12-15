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
import com.segment.analytics.platform.AndroidLifecycle
import com.segment.analytics.platform.DestinationExtension
import com.segment.analytics.platform.Extension

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