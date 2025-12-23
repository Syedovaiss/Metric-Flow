package com.ovais.metric_flow.domain

import android.os.Handler
import android.os.Looper
import android.util.Log

internal object ANRThreadMonitor : Thread("ANRThreadMonitor") {

    private const val TAG = "PerfMon"
    private const val TIMEOUT_MS = 5000L
    private val handler = Handler(Looper.getMainLooper())
    
    @Volatile
    private var running = false
    
    @Volatile
    private var isStarted = false

    @Synchronized
    fun startMonitoring() {
        if (isStarted) {
            Log.d(TAG, "ANRThreadMonitor already started, skipping")
            return
        }
        if (!running) {
            running = true
            isStarted = true
            try {
                start()
                Log.d(TAG, "ANRThreadMonitor started")
            } catch (e: IllegalThreadStateException) {
                // Thread already started
                running = false
                isStarted = false
                Log.e(TAG, "Failed to start ANRThreadMonitor", e)
            }
        }
    }

    @Synchronized
    fun stopMonitoring() {
        if (!isStarted) {
            return
        }
        running = false
        isStarted = false
        try {
            if (isAlive && !isInterrupted) {
                interrupt()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ANRThreadMonitor", e)
        }
        Log.d(TAG, "ANRThreadMonitor stopped")
    }

    override fun run() {
        while (running && !isInterrupted) {
            val start = System.currentTimeMillis()
            val pinged = BooleanArray(1) { false }

            handler.post {
                pinged[0] = true
            }

            try {
                var waited = 0L
                while (!pinged[0] && waited < TIMEOUT_MS) {
                    sleep(100)
                    waited += 100
                }

                if (!pinged[0]) {
                    Log.e(TAG, "ANR detected! Main thread blocked for > $TIMEOUT_MS ms")
                    // You can also report this to your SDK or Firebase Crashlytics
                }

                // Sleep a little before next ping
                sleep(500)
            } catch (e: InterruptedException) {
                // Thread interrupted — stop monitoring
                break
            }
        }
    }
}