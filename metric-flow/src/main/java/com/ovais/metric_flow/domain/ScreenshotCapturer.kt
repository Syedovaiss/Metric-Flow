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
        val activity = activityProvider.currentActivity
        val rootView = activity?.window?.decorView?.rootView ?: return
        val bitmap = createBitmap(rootView.width, rootView.height)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)
        val file = File(
            activity.applicationContext.cacheDir,
            buildCrashFileName(activity.localClassName)
        )
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, it)
        }

        Log.d("PerfMon", "Screenshot saved: ${file.path}")
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