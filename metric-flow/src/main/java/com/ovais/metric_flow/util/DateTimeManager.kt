package com.ovais.metric_flow.util

import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun interface DateTimeManager {
    fun getFormattedDateTime(date: Date, pattern: String): String?
}

class DefaultDateTimeManager : DateTimeManager {

    override fun getFormattedDateTime(date: Date, pattern: String): String? {
        return try {
            val dateFormat = SimpleDateFormat(
                pattern, Locale.getDefault()
            )
            dateFormat.format(date)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }
}