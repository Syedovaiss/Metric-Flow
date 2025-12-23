package com.ovais.metric_flow.core

import android.app.Application
import com.ovais.metric_flow.data.NetworkClientType
import com.ovais.metric_flow.data.PerformanceConfig
import com.ovais.metric_flow.domain.AppStartupTracker
import com.ovais.metric_flow.domain.CrashMonitorer
import com.ovais.metric_flow.domain.DefaultActivityProvider
import com.ovais.metric_flow.domain.DefaultAppStartupTracker
import com.ovais.metric_flow.domain.DefaultCrashMonitorer
import com.ovais.metric_flow.domain.DefaultFrameObserver
import com.ovais.metric_flow.domain.DefaultNetworkObserver
import com.ovais.metric_flow.domain.MemoryTracker
import com.ovais.metric_flow.domain.LogCatCollector
import com.ovais.metric_flow.domain.DefaultBatteryObserver
import com.ovais.metric_flow.domain.DefaultConnectivityObserver
import com.ovais.metric_flow.domain.DeviceInfoCollector
import com.ovais.metric_flow.util.MetricLoggingTree
import timber.log.Timber

interface MetricFlow {
    fun initialize(
        application: Application,
        config: PerformanceConfig
    )

    fun initialize(
        application: Application,
        config: PerformanceConfig,
        networkType: NetworkClientType
    )

    fun release()
}

/**
 * Factory for creating MetricFlow instances.
 * 
 * Usage:
 * ```
 * val metricFlow = MetricFlow.create()
 * metricFlow.initialize(application, config)
 * ```
 */
object MetricFlowFactory {
    /**
     * Creates a new MetricFlow instance.
     * 
     * @return A new MetricFlow instance ready to be initialized
     */
    @JvmStatic
    fun create(): MetricFlow = MetricFlowImpl()
}

class MetricFlowImpl : MetricFlow {
    @Volatile
    private var isInitialized = false
    private var networkType: NetworkClientType = NetworkClientType.OkHttp
    private var config = PerformanceConfig()
    private lateinit var application: Application
    private val crashMonitorer: CrashMonitorer by lazy { DefaultCrashMonitorer() }
    private val appStartupTracker: AppStartupTracker by lazy { DefaultAppStartupTracker() }
    private val activityProvider by lazy { DefaultActivityProvider() }
    private val frameObserver by lazy { DefaultFrameObserver() }
    private val networkObserver by lazy { DefaultNetworkObserver() }
    private val batteryObserver by lazy { DefaultBatteryObserver() }
    private val connectivityObserver by lazy { DefaultConnectivityObserver() }

    override fun initialize(
        application: Application,
        config: PerformanceConfig
    ) {
        // Input validation
        requireNotNull(application) { "Application cannot be null" }
        requireNotNull(config) { "PerformanceConfig cannot be null" }
        
        synchronized(this) {
            if (isInitialized) {
                Timber.tag("PerfMon").w("MetricFlow already initialized, skipping")
                return
            }
            isInitialized = true
        }
        
        try {
            validateConfig(config)
            initializeLogger()
            this.application = application
            this.config = config
            installModules()
        } catch (e: Exception) {
            synchronized(this) {
                isInitialized = false
            }
            Timber.tag("PerfMon").e(e, "Failed to initialize MetricFlow")
            throw e
        }
    }

    override fun initialize(
        application: Application,
        config: PerformanceConfig,
        networkType: NetworkClientType
    ) {
        // Input validation
        requireNotNull(application) { "Application cannot be null" }
        requireNotNull(config) { "PerformanceConfig cannot be null" }
        requireNotNull(networkType) { "NetworkClientType cannot be null" }
        
        synchronized(this) {
            if (isInitialized) {
                Timber.tag("PerfMon").w("MetricFlow already initialized, skipping")
                return
            }
            isInitialized = true
        }
        
        try {
            validateConfig(config)
            initializeLogger()
            this.application = application
            this.config = config
            this.networkType = networkType
            installModules()
        } catch (e: Exception) {
            synchronized(this) {
                isInitialized = false
            }
            Timber.tag("PerfMon").e(e, "Failed to initialize MetricFlow")
            throw e
        }
    }

    private fun validateConfig(config: PerformanceConfig) {
        require(config.memorySampleIntervalMs > 0) {
            "memorySampleIntervalMs must be greater than 0"
        }
        require(config.memoryLowMemoryTrimThresholdKb >= 0) {
            "memoryLowMemoryTrimThresholdKb must be non-negative"
        }
        require(config.memorySampleIntervalMs <= 300_000) {
            "memorySampleIntervalMs should not exceed 5 minutes (300000ms)"
        }
    }

    private fun installModules() {
        try {
            // Install ActivityProvider first as it's needed by other modules
            activityProvider.install(application)
        } catch (e: Exception) {
            Timber.tag("PerfMon").e(e, "Failed to install ActivityProvider")
        }
        
        if (config.enableCrashMonitoring) {
            try {
                crashMonitorer.install(
                    application = application,
                    canCaptureScreenshots = config.enableScreenshotCapture,
                    activityProvider = activityProvider
                )
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to install crash monitorer")
            }
        }
        if (config.enableAppStartupTracker) {
            try {
                appStartupTracker.install(activityProvider)
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to install app startup tracker")
            }
        }
        if (config.enableFrameObservation) {
            try {
                frameObserver.install()
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to install frame observer")
            }
        }
        if (config.enableNetworkObserver) {
            try {
                networkObserver.install(networkType)
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to install network observer")
            }
        }
        if (config.enableMemoryTracker) {
            try {
                MemoryTracker.install(
                    application = application,
                    config = MemoryTracker.Config(
                        sampleIntervalMs = config.memorySampleIntervalMs,
                        lowMemoryTrimThresholdKb = config.memoryLowMemoryTrimThresholdKb,
                        enableHeapDumpOnLowMemory = config.memoryEnableHeapDumpOnLowMemory,
                        heapDumpDirectory = null, // Can be configured later if needed
                        logSamples = config.memoryLogSamples
                    ),
                    listener = object : MemoryTracker.Listener {
                        override fun onSample(snapshot: MemoryTracker.MemorySnapshot) {
                            Timber.tag("PerfMon").d(
                                "Mem sample: used=%dKB pss=%dKB",
                                snapshot.runtimeUsedBytes / 1024,
                                snapshot.totalPssKb
                            )
                        }

                        override fun onLowMemory(
                            trimLevel: Int,
                            snapshot: MemoryTracker.MemorySnapshot
                        ) {
                            Timber.tag("PerfMon").w("Low memory event level=%s", trimLevel)
                        }

                        override fun onHeapDumpSaved(path: String) {
                            Timber.tag("PerfMon").i("Heap dump available at %s", path)
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to install memory tracker")
            }
        }
        if (config.enableLogsCapture) {
            try {
                LogCatCollector.install(
                    filterTag = "PerfMon",
                ) { logLine ->
                    Timber.tag("PerfMonLogs").e(logLine)
                }
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to install logcat collector")
            }
        }
        if (config.enableBatteryMonitoring) {
            try {
                batteryObserver.install(application)
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to install battery observer")
            }
        }
        if (config.enableConnectivityMonitoring) {
            try {
                connectivityObserver.install(application)
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to install connectivity observer")
            }
        }
        if (config.enableDeviceInfoCollection) {
            try {
                DeviceInfoCollector.logDeviceInfo(application)
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Failed to collect device info")
            }
        }
    }

    private fun initializeLogger() {
        // Only plant if no trees are already planted to avoid conflicts
        if (Timber.treeCount == 0) {
            Timber.plant(MetricLoggingTree())
        } else {
            // Add our tree alongside existing ones
            Timber.plant(MetricLoggingTree())
        }
    }

    override fun release() {
        val wasInitialized: Boolean
        synchronized(this) {
            wasInitialized = isInitialized
            if (!isInitialized) {
                Timber.tag("PerfMon").w("MetricFlow not initialized, nothing to release")
                return
            }
            isInitialized = false
        }
        
        // Release in reverse order of installation to minimize dependencies
        // Use separate try-catch for each to ensure all cleanup attempts happen
        if (wasInitialized) {
            try {
                connectivityObserver.release()
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error releasing connectivity observer")
            }
            try {
                batteryObserver.release()
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error releasing battery observer")
            }
            try {
                MemoryTracker.uninstall()
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error uninstalling memory tracker")
            }
            try {
                LogCatCollector.release()
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error releasing logcat collector")
            }
            try {
                networkObserver.release()
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error releasing network observer")
            }
            try {
                crashMonitorer.release()
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error releasing crash monitorer")
            }
            try {
                frameObserver.stop()
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error stopping frame observer")
            }
        }
    }
}