package com.ovais.metric_flow.observers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object LogCatCollector {

    private const val TAG = "LogCatCollector"
    private val isInstalled = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var logJob: Job? = null

    private var logFilterTag: String? = null
    private var logListener: ((String) -> Unit)? = null

    fun install(
        filterTag: String? = null,
        listener: ((String) -> Unit)? = null
    ) {
        if (isInstalled.get()) {
            Log.d(TAG, "LogCatCollector already installed")
            return
        }
        isInstalled.set(true)
        logFilterTag = filterTag
        logListener = listener
        startCollecting()
        Log.d(TAG, "LogCatCollector installed (capturing W & E only)")
    }

    private fun startCollecting() {
        logJob = coroutineScope.launch {
            try {
                val command = if (logFilterTag != null) {
                    listOf("logcat", "-v", "time", "${logFilterTag}:W", "*:S")
                } else {
                    listOf("logcat", "-v", "time", "*:W") // Only warnings and errors
                }

                val process = ProcessBuilder(command).redirectErrorStream(true).start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                reader.useLines { lines ->
                    lines.forEach { logLine ->
                        if (shouldInclude(logLine)) {
                            val formattedLog = "[${timestamp()}] $logLine"
                            logListener?.invoke(formattedLog)
                        }
                    }
                }

                process.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading Logcat: ${e.message}", e)
            }
        }
    }

    fun release() {
        if (!isInstalled.get()) return
        logJob?.cancel()
        coroutineScope.cancel()
        isInstalled.set(false)
        Log.d(TAG, "LogCatCollector stopped")
    }

    private fun shouldInclude(logLine: String): Boolean {
        return logLine.contains(" W/") || logLine.contains(" E/")
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }
}