package com.ovais.metric_flow.data

data class PerformanceConfig(
    val enableCrashMonitoring: Boolean = true,
    val enableAppStartupTracker: Boolean = true,
    val enableFrameObservation: Boolean = true,
    val enableMemoryTracker: Boolean = true,
    val enableScreenshotCapture: Boolean = true,
    val enableNetworkObserver: Boolean = true,
    val enableLogsCapture: Boolean = true,
    val enableBatteryMonitoring: Boolean = true,
    val enableConnectivityMonitoring: Boolean = true,
    val enableDeviceInfoCollection: Boolean = true,
    // MemoryTracker configuration
    val memorySampleIntervalMs: Long = 5_000L,
    val memoryLowMemoryTrimThresholdKb: Int = 50 * 1024, // 50 MB in KB
    val memoryEnableHeapDumpOnLowMemory: Boolean = false,
    val memoryLogSamples: Boolean = true
)


class PerformanceConfigBuilder {
    var enableCrashMonitoring: Boolean = true
    var enableAppStartupTracker: Boolean = true
    var enableFrameObservation: Boolean = true
    var enableMemoryTracker: Boolean = true
    var enableScreenshotCapture: Boolean = true
    var enableNetworkObserver: Boolean = true
    var enableLogsCapture: Boolean = true
    var enableBatteryMonitoring: Boolean = true
    var enableConnectivityMonitoring: Boolean = true
    var enableDeviceInfoCollection: Boolean = true
    
    // MemoryTracker configuration
    var memorySampleIntervalMs: Long = 5_000L
    var memoryLowMemoryTrimThresholdKb: Int = 50 * 1024
    var memoryEnableHeapDumpOnLowMemory: Boolean = false
    var memoryLogSamples: Boolean = true

    fun build(): PerformanceConfig {
        return PerformanceConfig(
            enableCrashMonitoring,
            enableAppStartupTracker,
            enableFrameObservation,
            enableMemoryTracker,
            enableScreenshotCapture,
            enableNetworkObserver,
            enableLogsCapture,
            enableBatteryMonitoring,
            enableConnectivityMonitoring,
            enableDeviceInfoCollection,
            memorySampleIntervalMs,
            memoryLowMemoryTrimThresholdKb,
            memoryEnableHeapDumpOnLowMemory,
            memoryLogSamples
        )
    }
}

fun performanceConfig(block: PerformanceConfigBuilder.() -> Unit): PerformanceConfig {
    val builder = PerformanceConfigBuilder()
    builder.block()
    return builder.build()
}