package com.segment.analytics

import android.app.Activity
import android.os.Bundle
import com.segment.analytics.integrations.BasePayload

private class PluginExecutor {
    private val plugins: Map<Extension.Type, PluginMediator> = mapOf(
            Extension.Type.Before to PluginMediator(emptyList()),
            Extension.Type.SourceEnrichment to PluginMediator(emptyList()),
            Extension.Type.DestinationEnrichment to PluginMediator(emptyList()),
            Extension.Type.Destination to PluginMediator(emptyList()),
            Extension.Type.After to PluginMediator(emptyList())
    )

    private fun execute(payload: BasePayload, type: Extension.Type) {
        val pluginMediator = plugins[type] ?: throw IllegalArgumentException("unknown extension type")
        pluginMediator.execute()
    }

    fun add(extension: Extension) {
        extension.type
    }
    fun remove(extension: Extension) {}
}

private class PluginMediator(val extensions: List<Extension>) {
    fun execute() {

    }
}

private class EventOrchestrator {
    //  Might just be a static function
}

interface Extension {
    enum class Type {
        Before, SourceEnrichment, DestinationEnrichment, Destination, After
    }

    val type: Type

    fun preFlight(): Boolean

    fun action(payload: BasePayload): BasePayload

    fun track(payload: BasePayload): BasePayload
    fun identify(payload: BasePayload): BasePayload
    fun screen(payload: BasePayload): BasePayload
    fun group(payload: BasePayload): BasePayload
    fun alias(payload: BasePayload): BasePayload
    fun page(payload: BasePayload): BasePayload

}

interface LifecycleExtension: Extension {
    fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {}
    fun onActivityStarted(activity: Activity) {}
    fun onActivityResumed(activity: Activity) {}
    fun onActivityPaused(activity: Activity) {}
    fun onActivityStopped(activity: Activity) {}
    fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    fun onActivityDestroyed(activity: Activity) {}
}