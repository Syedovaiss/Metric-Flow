package com.ovais.metric_flow.domain

import android.util.Log

fun interface AppStartupTracker {
    fun install(activityProvider: ActivityProvider)
}

class DefaultAppStartupTracker : AppStartupTracker {
    private var startTime = 0L
    private var isFirstFrameDrawn = false

    override fun install(activityProvider: ActivityProvider) {
        startTime = System.currentTimeMillis()
        activityProvider.addOnActivityResumedListener { onFirstFrameDrawn() }
    }

    private fun onFirstFrameDrawn() {
        if (isFirstFrameDrawn) return
        val duration = System.currentTimeMillis() - startTime
        Log.d("PerfMon", "Startup time: $duration ms")
        isFirstFrameDrawn = true
    }
}