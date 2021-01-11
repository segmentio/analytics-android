package com.segment.analytics

import com.segment.analytics.integrations.BasePayload

fun interface NewMiddleware {
    fun run(payload: BasePayload?): BasePayload?
}

class MiddlewareAdapter(val middleware: Middleware): NewMiddleware {
    override fun run(payload: BasePayload?): BasePayload? {
        var nextPayload: BasePayload? = null
        val chain = object: Middleware.Chain {
            override fun payload(): BasePayload? {
                return payload
            }

            override fun proceed(payload: BasePayload?) {
                nextPayload = payload
            }
        }
        middleware.intercept(chain) // this will execute proceed in chain and set nextPayload
        return nextPayload
    }
}