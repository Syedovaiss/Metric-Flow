package com.ovais.metric_flow.util

import timber.log.Timber


class MetricLoggingTree : Timber.DebugTree() {

    private companion object {
        private const val TAG = "Metric-Flow-Logs =>"
    }

    override fun i(message: String?, vararg args: Any?) {
        val taggedMessage = "$TAG $message"
        super.i(taggedMessage, *args)
    }

    override fun e(message: String?, vararg args: Any?) {
        val taggedMessage = "$TAG $message"
        super.e(taggedMessage, *args)
    }
}