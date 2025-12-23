package com.ovais.metric_flow.data

data class PerformanceConfig(
    val enableCrashMonitoring: Boolean = true,
    val enableAppStartupTracker: Boolean = true,
    val enableFrameObservation: Boolean = true,
    val enableMemoryTracker: Boolean = true,
    val enableScreenshotCapture: Boolean = true,
    val enableNetworkObserver: Boolean = true,
    val enableLogsCapture: Boolean = true
)


class PerformanceConfigBuilder {
    var enableCrashMonitoring: Boolean = true
    var enableAppStartupTracker: Boolean = true
    var enableFrameObservation: Boolean = true
    var enableMemoryTracker: Boolean = true
    var enableScreenshotCapture: Boolean = true
    var enableNetworkObserver: Boolean = true
    var enableLogsCapture: Boolean = true

    fun build(): PerformanceConfig {
        return PerformanceConfig(
            enableCrashMonitoring,
            enableAppStartupTracker,
            enableFrameObservation,
            enableMemoryTracker,
            enableScreenshotCapture,
            enableNetworkObserver,
            enableLogsCapture
        )
    }
}

fun performanceConfig(block: PerformanceConfigBuilder.() -> Unit): PerformanceConfig {
    val builder = PerformanceConfigBuilder()
    builder.block()
    return builder.build()
}