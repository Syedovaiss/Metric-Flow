package com.ovais.metric_flow.domain

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * MemoryTracker
 *
 * - install(application, config) to start
 * - uninstall() to stop
 * - register a listener to receive periodic MemorySnapshot objects
 *
 * Non-blocking. Uses a background scheduler for periodic sampling.
 */
object MemoryTracker {

    data class Config(
        val sampleIntervalMs: Long = 5_000L,
        val lowMemoryTrimThresholdKb: Int = 50 * 1024, // 50 MB in KB
        val enableHeapDumpOnLowMemory: Boolean = false,
        val heapDumpDirectory: File? = null,
        val logSamples: Boolean = true
    )

    data class MemorySnapshot(
        val timestamp: Long,
        val totalPssKb: Int,
        val dalvikPssKb: Int,
        val nativePssKb: Int,
        val otherPssKb: Int,
        val runtimeUsedBytes: Long,
        val runtimeFreeBytes: Long,
        val runtimeTotalBytes: Long,
        val availMemBytes: Long,
        val totalMemBytes: Long,
        val lowMemory: Boolean,
        val thresholdBytes: Long
    )

    interface Listener {
        fun onSample(snapshot: MemorySnapshot)
        fun onLowMemory(trimLevel: Int, snapshot: MemorySnapshot) = Unit
        fun onHeapDumpSaved(path: String) = Unit
    }

    @Volatile
    private var installed = false
    private var application: Application? = null
    private var config: Config = Config()
    private var listener: Listener? = null
    
    private val installLock = Any()

    private val scheduler = ScheduledThreadPoolExecutor(1).apply {
        removeOnCancelPolicy = true
    }
    private var scheduledFuture: ScheduledFuture<*>? = null

    private var dumpHandler: Handler? = null

    private val componentCallbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            try {
                val snap = sampleNow() ?: return
                Timber.tag("MemoryTracker")
                    .d("onTrimMemory level=%s lowMemory=%s", level, snap.lowMemory)
                listener?.onLowMemory(level, snap)

                // Optional heap dump on low memory
                if (config.enableHeapDumpOnLowMemory && isSevereTrim(level)) {
                    performHeapDumpAsync()
                }
            } catch (t: Throwable) {
                Timber.tag("MemoryTracker").e(t, "MemoryTracker: onTrimMemory error")
            }
        }

        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) = Unit

        override fun onLowMemory() {
            try {
                val snap = sampleNow() ?: return
                Timber.tag("MemoryTracker").w("onLowMemory called")
                listener?.onLowMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, snap)
                if (config.enableHeapDumpOnLowMemory) performHeapDumpAsync()
            } catch (t: Throwable) {
                Timber.tag("MemoryTracker").e(t, "MemoryTracker: onLowMemory error")
            }
        }
    }

    fun install(
        application: Application,
        config: Config = Config(),
        listener: Listener? = null
    ) {
        requireNotNull(application) { "Application cannot be null" }
        require(config.sampleIntervalMs > 0) { "sampleIntervalMs must be greater than 0" }
        require(config.lowMemoryTrimThresholdKb >= 0) { "lowMemoryTrimThresholdKb must be non-negative" }
        
        synchronized(installLock) {
            if (installed) {
                Timber.tag("MemoryTracker").w("MemoryTracker already installed, skipping")
                return
            }
            installed = true
        }
        
        this.application = application
        this.config = config
        this.listener = listener

        Timber.tag("MemoryTracker").d("install: sampleIntervalMs=%d", config.sampleIntervalMs)

        try {
            // register callbacks
            application.registerComponentCallbacks(componentCallbacks)

            // prepare dump handler thread if heap dump enabled
            if (config.enableHeapDumpOnLowMemory) {
                val t = HandlerThread("MemoryTrackerDump").apply { start() }
                dumpHandler = Handler(t.looper)
            }

            // schedule periodic sampling
            scheduledFuture = scheduler.scheduleWithFixedDelay({
                try {
                    val snap = sampleNow()
                    if (snap != null) {
                        if (config.logSamples) Timber.tag("MemoryTracker").d("Memory sample: %s", snap)
                        listener?.onSample(snap)
                        if (snap.lowMemory && config.enableHeapDumpOnLowMemory) {
                            performHeapDumpAsync()
                        }
                    }
                } catch (t: Throwable) {
                    Timber.tag("MemoryTracker").e(t, "MemoryTracker periodic sample failed")
                }
            }, 0L, config.sampleIntervalMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            synchronized(installLock) {
                installed = false
            }
            Timber.tag("MemoryTracker").e(e, "Failed to install MemoryTracker")
            throw e
        }
    }

    fun uninstall() {
        synchronized(installLock) {
            if (!installed) {
                return
            }
            installed = false
        }
        
        try {
            application?.unregisterComponentCallbacks(componentCallbacks)
        } catch (e: IllegalArgumentException) {
            // Callbacks not registered - this is OK
            Timber.tag("MemoryTracker").d("ComponentCallbacks already unregistered")
        } catch (e: Throwable) {
            Timber.tag("MemoryTracker").e(e, "Error unregistering ComponentCallbacks")
        }
        
        scheduledFuture?.cancel(true)
        scheduledFuture = null
        
        try {
            dumpHandler?.looper?.thread?.interrupt()
            dumpHandler = null
        } catch (e: Throwable) {
            Timber.tag("MemoryTracker").e(e, "Error interrupting dump handler thread")
        }
        
        synchronized(installLock) {
            listener = null
            application = null
        }
        Timber.tag("MemoryTracker").d("uninstalled")
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    /**
     * Synchronous sample of current memory state. Safe to call on background thread.
     * Returns null if not installed or if sampling failed.
     */
    fun sampleNow(): MemorySnapshot? {
        val app = application ?: return null
        try {
            val runtime = Runtime.getRuntime()
            val runtimeUsed = runtime.totalMemory() - runtime.freeMemory()
            val runtimeFree = runtime.freeMemory()
            val runtimeTotal = runtime.totalMemory()

            // Debug.MemoryInfo (PSS)
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            val dalvikPssKb = memInfo.dalvikPss
            val nativePssKb = memInfo.nativePss
            val otherPssKb = memInfo.otherPss
            val totalPssKb = dalvikPssKb + nativePssKb + otherPssKb

            // ActivityManager.MemoryInfo for overall system memory
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val amInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(amInfo)

            val lowMemory = amInfo.lowMemory || totalPssKb >= config.lowMemoryTrimThresholdKb
            val thresholdBytes = amInfo.threshold
            val availMemBytes = amInfo.availMem
            val totalMemBytes = amInfo.totalMem // may be 0 pre-API 16 on some devices

            return MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                totalPssKb = max(0, totalPssKb),
                dalvikPssKb = max(0, dalvikPssKb),
                nativePssKb = max(0, nativePssKb),
                otherPssKb = max(0, otherPssKb),
                runtimeUsedBytes = runtimeUsed,
                runtimeFreeBytes = runtimeFree,
                runtimeTotalBytes = runtimeTotal,
                availMemBytes = availMemBytes,
                totalMemBytes = totalMemBytes,
                lowMemory = lowMemory,
                thresholdBytes = thresholdBytes
            )
        } catch (t: Throwable) {
            Timber.tag("MemoryTracker").e(t, "sampleNow failed")
            return null
        }
    }

    // --- heap dump helpers ---
    private fun performHeapDumpAsync() {
        val cfg = config
        val dir = cfg.heapDumpDirectory ?: application?.cacheDir
        if (dir == null) {
            Timber.tag("MemoryTracker").w("Heap dump requested but no directory available")
            return
        }
        val handler = dumpHandler ?: run {
            Timber.tag("MemoryTracker").w("Heap dump requested but dumpHandler not initialized")
            return
        }

        handler.post {
            val file = createDumpFile(dir)
            try {
                // Note: dump may be heavy and cause ANR if used carelessly.
                Debug.dumpHprofData(file.absolutePath)
                Timber.tag("MemoryTracker").i("Heap dump saved to %s", file.absolutePath)
                listener?.onHeapDumpSaved(file.absolutePath)
            } catch (io: IOException) {
                Timber.tag("MemoryTracker")
                    .e(io, "Failed to write heap dump to ${file.absolutePath}")
            } catch (r: RuntimeException) {
                // Dump can throw on some devices/VMs
                Timber.tag("MemoryTracker").e(r, "Heap dump runtime error")
            } catch (t: Throwable) {
                Timber.tag("MemoryTracker").e(t, "Unexpected error while dumping heap")
            }
        }
    }

    private fun createDumpFile(dir: File): File {
        if (!dir.exists()) dir.mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "metricflow_heap_$ts.hprof")
    }

    private fun isSevereTrim(level: Int): Boolean {
        return level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
    }
}