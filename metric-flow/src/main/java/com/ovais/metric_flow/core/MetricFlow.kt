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
import com.ovais.metric_flow.observers.LogCatCollector
import com.ovais.metric_flow.util.MetricLoggingTree
import timber.log.Timber

interface MetricFlow {
    fun initialize(
        application: Application,
        config: PerformanceConfig,
        firebaseSDKEnabled: Boolean
    )

    fun initialize(
        application: Application,
        config: PerformanceConfig,
        networkType: NetworkClientType,
        firebaseSDKEnabled: Boolean
    )

    fun release()
}

class MetricFlowImpl : MetricFlow {
    private var networkType: NetworkClientType = NetworkClientType.OkHttp
    private var config = PerformanceConfig()
    private lateinit var application: Application
    private val crashMonitorer: CrashMonitorer by lazy { DefaultCrashMonitorer() }
    private val appStartupTracker: AppStartupTracker by lazy { DefaultAppStartupTracker() }
    private val activityProvider by lazy { DefaultActivityProvider() }
    private val frameObserver by lazy { DefaultFrameObserver() }
    private val networkObserver by lazy { DefaultNetworkObserver() }

    override fun initialize(
        application: Application,
        config: PerformanceConfig,
        firebaseSDKEnabled: Boolean
    ) {
        initializeLogger()
        if (::application.isInitialized) return
        this.application = application
        this.config = config
        installModules()
    }

    override fun initialize(
        application: Application,
        config: PerformanceConfig,
        networkType: NetworkClientType,
        firebaseSDKEnabled: Boolean
    ) {
        initializeLogger()
        if (::application.isInitialized) return
        this.application = application
        this.config = config
        this.networkType = networkType
        installModules()
    }

    private fun installModules() {
        if (config.enableCrashMonitoring) {
            crashMonitorer.install(
                canCaptureScreenshots = config.enableScreenshotCapture,
                activityProvider = activityProvider
            )
        }
        if (config.enableAppStartupTracker) {
            appStartupTracker.install(activityProvider)
        }
        if (config.enableFrameObservation) {
            frameObserver.install()
        }
        if (config.enableNetworkObserver) {
            networkObserver.install(networkType)
        }
        if (config.enableMemoryTracker) {
            MemoryTracker.install(
                application = application,
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
        }
        if (config.enableLogsCapture) {
            LogCatCollector.install(
                filterTag = "PerfMon",
            ) { logLine ->
                Timber.tag("PerfMonLogs").e(logLine)
            }
        }
    }

    private fun initializeLogger() {
        Timber.plant(MetricLoggingTree())
    }

    override fun release() {
        crashMonitorer.release()
        LogCatCollector.release()
        MemoryTracker.uninstall()
    }
}