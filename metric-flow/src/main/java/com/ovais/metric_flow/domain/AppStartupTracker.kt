package com.ovais.metric_flow.domain

import android.util.Log

fun interface AppStartupTracker {
    fun install(activityProvider: ActivityProvider)
}

class DefaultAppStartupTracker : AppStartupTracker {
    @Volatile
    private var isInstalled = false
    
    @Volatile
    private var startTime = 0L
    
    @Volatile
    private var isFirstFrameDrawn = false

    override fun install(activityProvider: ActivityProvider) {
        requireNotNull(activityProvider) { "ActivityProvider cannot be null" }
        
        synchronized(this) {
            if (isInstalled) {
                Log.w("PerfMon", "AppStartupTracker already installed, skipping")
                return
            }
            isInstalled = true
        }
        
        startTime = System.currentTimeMillis()
        activityProvider.addOnActivityResumedListener { activity ->
            if (activity != null) {
                onFirstFrameDrawn()
            }
        }
    }

    private fun onFirstFrameDrawn() {
        if (isFirstFrameDrawn) return
        
        synchronized(this) {
            if (isFirstFrameDrawn) return
            isFirstFrameDrawn = true
        }
        
        val duration = System.currentTimeMillis() - startTime
        if (duration >= 0) {
            Log.d("PerfMon", "Startup time: $duration ms")
        } else {
            Log.w("PerfMon", "Invalid startup time calculation: $duration")
        }
    }
}