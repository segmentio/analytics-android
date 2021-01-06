package com.segment.analytics.platform.extensions

enum class LogType(val level: Int) {
    ERROR(0),       // Not Verbose
    WARNING(1),     // Semi-verbose
    INFO(2)         // Verbose
}

