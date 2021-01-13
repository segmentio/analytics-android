package com.segment.analytics.platform

import com.segment.analytics.Analytics
import com.segment.analytics.Middleware
import com.segment.analytics.MiddlewareAdapter
import com.segment.analytics.NewMiddleware
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload

// A wrapper class to transform existing middleware into plugins
class MiddlewarePluginAdapter: EventPlugin {

    companion object {
        private var nextId = 1
        fun nextId(): Int {
            return nextId++
        }
    }

    override val type: Plugin.Type = Plugin.Type.Enrichment
    override val name: String = "middleware-${nextId()}"
    override var analytics: Analytics? = null
    private val underlying: NewMiddleware

    constructor(underlying: Middleware) {
        this.underlying = MiddlewareAdapter(underlying)
    }

    constructor(underlying: NewMiddleware) {
        this.underlying = underlying
    }

    override fun track(payload: TrackPayload?): BasePayload? {
        return underlying.run(payload)
    }

    override fun identify(payload: IdentifyPayload?): BasePayload? {
        return underlying.run(payload)
    }

    override fun screen(payload: ScreenPayload?): BasePayload? {
        return underlying.run(payload)
    }

    override fun group(payload: GroupPayload?): BasePayload? {
        return underlying.run(payload)
    }

    override fun alias(payload: AliasPayload?): BasePayload? {
        return underlying.run(payload)
    }
}