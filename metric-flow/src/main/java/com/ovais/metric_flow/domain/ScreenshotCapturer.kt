package com.ovais.metric_flow.domain

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.core.graphics.createBitmap
import com.ovais.metric_flow.util.DatePattern.YYYY_MM_DD_HH_MM_SS
import com.ovais.metric_flow.util.DefaultDateTimeManager
import java.io.File
import java.io.FileOutputStream
import java.util.Date

fun interface ScreenshotCapturer {
    fun install(activityProvider: ActivityProvider)
}

class DefaultScreenshotCapturer : ScreenshotCapturer {

    private companion object {
        private const val PREFIX = "metric_flow"
        private const val SEPARATOR = "-"
        private const val FILE_EXTENSION = ".png"
    }

    private val dateTimeManager by lazy { DefaultDateTimeManager() }

    override fun install(activityProvider: ActivityProvider) {
        try {
            val activity = activityProvider.currentActivity ?: run {
                Log.w("PerfMon", "No current activity available for screenshot")
                return
            }
            
            val window = activity.window ?: run {
                Log.w("PerfMon", "Activity window is null")
                return
            }
            
            val rootView = window.decorView?.rootView ?: run {
                Log.w("PerfMon", "Root view is null")
                return
            }
            
            val width = rootView.width
            val height = rootView.height
            
            if (width <= 0 || height <= 0) {
                Log.w("PerfMon", "Invalid view dimensions: ${width}x${height}")
                return
            }
            
            val bitmap = try {
                createBitmap(width, height)
            } catch (e: OutOfMemoryError) {
                Log.e("PerfMon", "Out of memory creating bitmap", e)
                return
            }
            
            val canvas = Canvas(bitmap)
            try {
                rootView.draw(canvas)
            } catch (e: Exception) {
                Log.e("PerfMon", "Error drawing view to canvas", e)
                bitmap.recycle()
                return
            }
            
            val cacheDir = activity.applicationContext.cacheDir
            if (cacheDir == null || !cacheDir.exists()) {
                Log.e("PerfMon", "Cache directory not available")
                bitmap.recycle()
                return
            }
            
            val fileName = try {
                buildCrashFileName(activity.localClassName ?: "Unknown")
            } catch (e: Exception) {
                Log.e("PerfMon", "Error building file name", e)
                bitmap.recycle()
                return
            }
            
            val file = File(cacheDir, fileName)
            try {
                FileOutputStream(file).use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)) {
                        Log.e("PerfMon", "Failed to compress bitmap")
                    }
                }
                Log.d("PerfMon", "Screenshot saved: ${file.path}")
            } catch (e: Exception) {
                Log.e("PerfMon", "Error saving screenshot", e)
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e("PerfMon", "Unexpected error capturing screenshot", e)
        }
    }

    private fun buildCrashFileName(className: String) = buildString {
        append(PREFIX)
        append(SEPARATOR)
        append(className)
        append(SEPARATOR)
        append(
            dateTimeManager.getFormattedDateTime(
                date = Date(),
                pattern = YYYY_MM_DD_HH_MM_SS
            )
        )
        append(FILE_EXTENSION)
    }
}