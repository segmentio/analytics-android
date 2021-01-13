package com.segment.analytics.platform.plugins

import com.segment.analytics.Analytics
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.platform.EventPlugin
import com.segment.analytics.platform.Plugin

enum class LogType(val level: Int) {
    ERROR(0),       // Not Verbose
    WARNING(1),     // Semi-verbose
    INFO(2)         // Verbose
}

data class LogMessage(
        val type: LogType,
        val message: String,
        val event: BasePayload?
)

// Simple logger plugin
open class Logger(override val name: String) : EventPlugin {

    override val type: Plugin.Type = Plugin.Type.Utility
    override var analytics: Analytics? = null

    open var filterType: LogType = LogType.INFO

    private val messages = mutableListOf<LogMessage>()

    open fun log(type: LogType, message: String, event: BasePayload?) {
        print("$type -- Message: $message")
        val m = LogMessage(type, message, event)
        messages.add(m)
    }

    open fun flush() {
        print("Flushing All Logs")
        for (message in messages) {
            if (message.type.level <= filterType.level) {
                print("[${message.type}] ${message.message}")
            }
        }
        messages.clear()
    }
}

fun Analytics.log(message: String, event: BasePayload? = null, type: LogType? = null) {
    this.timeline.applyClosure { plugin: Plugin ->
        if (plugin is Logger) {
            plugin.log(type ?: plugin.filterType, message, event)
        }
    }
}

fun Analytics.logFlush() {
    this.timeline.applyClosure { plugin: Plugin ->
        if (plugin is Logger) {
            plugin.flush()
        }
    }
}