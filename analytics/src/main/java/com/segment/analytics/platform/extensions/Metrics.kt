package com.segment.analytics.platform.extensions

import com.segment.analytics.integrations.BasePayload
import java.util.Date

enum class MetricType(val type: Int) {
    Counter(0), // Not Verbose
    Gauge(1)    // Semi-verbose
}

data class Metric(
        var eventName: String = "",
        var metricName: String = "",
        var value: Double = 0.0,
        var tags: List<String> = emptyList(),
        var type: MetricType = MetricType.Counter,
        var timestamp: Date = Date()
)

fun BasePayload.addMetric(type: MetricType, name: String, value: Double, tags: List<String>, timestamp: Date) {
    val metric = Metric(eventName = type().toString(), metricName = name, value = value, tags = tags, type = type, timestamp = timestamp) // todo extension-name doesnt make much sense here...
    var metrics = mutableListOf<Metric>()
    if (containsKey("metrics")) {
        metrics = get("metrics") as? MutableList<Metric> ?: mutableListOf()
    }
    metrics.add(metric)
    put("metrics", metrics)
}