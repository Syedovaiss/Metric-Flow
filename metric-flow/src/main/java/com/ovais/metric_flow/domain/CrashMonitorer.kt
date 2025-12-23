package com.ovais.metric_flow.domain

import android.app.Application
import android.util.Log
import timber.log.Timber

interface CrashMonitorer {
    fun install(
        application: Application,
        canCaptureScreenshots: Boolean,
        activityProvider: ActivityProvider
    )

    fun release()
}

class DefaultCrashMonitorer : CrashMonitorer, Thread.UncaughtExceptionHandler {
    @Volatile
    private var isInstalled = false
    
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var canCaptureScreenshots = false
    private lateinit var application: Application
    private val screenshotCapturer: ScreenshotCapturer by lazy { DefaultScreenshotCapturer() }
    private lateinit var activityProvider: ActivityProvider
    
    override fun install(
        application: Application,
        canCaptureScreenshots: Boolean,
        activityProvider: ActivityProvider
    ) {
        requireNotNull(application) { "Application cannot be null" }
        requireNotNull(activityProvider) { "ActivityProvider cannot be null" }
        
        synchronized(this) {
            if (isInstalled) {
                Timber.tag("PerfMon").w("CrashMonitorer already installed, skipping")
                return
            }
            isInstalled = true
        }
        
        this.application = application
        this.canCaptureScreenshots = canCaptureScreenshots
        this.activityProvider = activityProvider
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        try {
            ANRThreadMonitor.startMonitoring()
            Thread.setDefaultUncaughtExceptionHandler(this)
        } catch (e: Exception) {
            synchronized(this) {
                isInstalled = false
            }
            Timber.tag("PerfMon").e(e, "Failed to install crash monitorer")
            throw e
        }
    }

    override fun uncaughtException(thread: Thread, e: Throwable) {
        Timber.tag("PerfMon").e(e, "Crash detected")
        if (canCaptureScreenshots) {
            if (::application.isInitialized) {
                try {
                    screenshotCapturer.install(activityProvider)
                } catch (ex: Exception) {
                    Timber.tag("PerfMon").e(ex, "Failed to capture screenshot")
                }
            }
        }
        defaultHandler?.uncaughtException(thread, e)
    }

    override fun release() {
        synchronized(this) {
            if (!isInstalled) {
                return
            }
            isInstalled = false
        }
        
        try {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler == this) {
                Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
            }
        } catch (e: Exception) {
            Timber.tag("PerfMon").e(e, "Error restoring default exception handler")
        }
        
        try {
            ANRThreadMonitor.stopMonitoring()
        } catch (e: Exception) {
            Timber.tag("PerfMon").e(e, "Error stopping ANR monitor")
        }
        
        defaultHandler = null
    }
}