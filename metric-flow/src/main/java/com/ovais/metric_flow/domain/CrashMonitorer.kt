package com.ovais.metric_flow.domain

import android.app.Application
import android.util.Log

interface CrashMonitorer {
    fun install(
        canCaptureScreenshots: Boolean,
        activityProvider: ActivityProvider
    )

    fun release()
}

class DefaultCrashMonitorer : CrashMonitorer, Thread.UncaughtExceptionHandler {
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var canCaptureScreenshots = false
    private lateinit var application: Application
    private val screenshotCapturer: ScreenshotCapturer by lazy { DefaultScreenshotCapturer() }
    private lateinit var activityProvider: ActivityProvider
    override fun install(
        canCaptureScreenshots: Boolean,
        activityProvider: ActivityProvider
    ) {
        ANRThreadMonitor.startMonitoring()
        this.canCaptureScreenshots = canCaptureScreenshots
        this.activityProvider = activityProvider
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, e: Throwable) {
        Log.e("PerfMon", "Crash detected", e)
        if (canCaptureScreenshots) {
            if (::application.isInitialized) return
            screenshotCapturer.install(activityProvider)
        }
        defaultHandler?.uncaughtException(thread, e)
    }

    override fun release() {
        ANRThreadMonitor.stopMonitoring()
    }
}