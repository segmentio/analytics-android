package com.segment.analytics.platform.plugins

import com.segment.analytics.DateSerializer
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.internal.Utils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Date

enum class MetricType(val type: Int) {
    Counter(0), // Not Verbose
    Gauge(1)    // Semi-verbose
}

@Serializable
data class Metric(
        var eventName: String = "",
        var metricName: String = "",
        var value: Double = 0.0,
        var tags: List<String> = emptyList(),
        var type: MetricType = MetricType.Counter,
        @Serializable(with = DateSerializer::class) var timestamp: Date = Date()
) {
    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
                "eventName" to eventName,
                "metricName" to metricName,
                "value" to value,
                "tags" to tags,
                "type" to type.toString(),
                "timestamp" to Utils.toISO8601String(timestamp)
        )
    }
}

fun BasePayload.addMetric(type: MetricType, name: String, value: Double, tags: List<String>, timestamp: Date) {
    val metric = Metric(eventName = type().toString(), metricName = name, value = value, tags = tags, type = type, timestamp = timestamp)
    var metrics = mutableListOf<Map<String, Any>>()
    if (containsKey("metrics")) {
        metrics = get("metrics") as? MutableList<Map<String, Any>> ?: mutableListOf()
    }
    metrics.add(metric.toMap())
    put("metrics", metrics)
}