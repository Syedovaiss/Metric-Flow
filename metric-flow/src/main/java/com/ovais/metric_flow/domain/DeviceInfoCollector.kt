package com.ovais.metric_flow.domain

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber

/**
 * Device and app information collector.
 * Collects device specs, app version, and system information.
 */
object DeviceInfoCollector {

    data class DeviceInfo(
        val deviceModel: String,
        val manufacturer: String,
        val androidVersion: String,
        val sdkVersion: Int,
        val appVersion: String?,
        val appVersionCode: Long?,
        val totalMemory: Long,
        val availableMemory: Long,
        val cpuCores: Int,
        val screenResolution: String
    )

    /**
     * Collects comprehensive device and app information.
     */
    fun collect(context: Context): DeviceInfo {
        requireNotNull(context) { "Context cannot be null" }
        
        val packageManager = context.packageManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: throw IllegalStateException("ActivityManager not available")
        
        val memoryInfo = ActivityManager.MemoryInfo()
        try {
            activityManager.getMemoryInfo(memoryInfo)
        } catch (e: Exception) {
            Timber.tag("DeviceInfoCollector").e(e, "Error getting memory info")
        }

        val packageInfo: PackageInfo? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }

        val displayMetrics = try {
            context.resources.displayMetrics
        } catch (e: Exception) {
            Timber.tag("DeviceInfoCollector").e(e, "Error getting display metrics")
            null
        }
        val screenResolution = displayMetrics?.let {
            "${it.widthPixels}x${it.heightPixels}"
        } ?: "Unknown"

        val appVersion = packageInfo?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.versionName
            } else {
                @Suppress("DEPRECATION")
                it.versionName
            }
        }

        val appVersionCode = packageInfo?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                it.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                it.versionCode.toLong()
            }
        }

        return DeviceInfo(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            appVersion = appVersion,
            appVersionCode = appVersionCode,
            totalMemory = memoryInfo.totalMem,
            availableMemory = memoryInfo.availMem,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            screenResolution = screenResolution
        )
    }

    /**
     * Logs device information.
     */
    fun logDeviceInfo(context: Context) {
        val info = collect(context)
        Timber.tag("PerfMon").i(
            """
            Device Info:
            Model: ${info.manufacturer} ${info.deviceModel}
            Android: ${info.androidVersion} (SDK ${info.sdkVersion})
            App Version: ${info.appVersion} (${info.appVersionCode})
            Memory: ${info.availableMemory / 1024 / 1024}MB / ${info.totalMemory / 1024 / 1024}MB available
            CPU Cores: ${info.cpuCores}
            Screen: ${info.screenResolution}
            """.trimIndent()
        )
    }
}

