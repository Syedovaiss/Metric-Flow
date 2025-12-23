package com.ovais.metricflow

import android.app.Application
import com.ovais.metric_flow.core.MetricFlowFactory
import com.ovais.metric_flow.data.performanceConfig


class MyApp : Application() {

    private val metricFlow by lazy { MetricFlowFactory.create() }

    override fun onCreate() {
        super.onCreate()
        metricFlow.initialize(
            application = this@MyApp,
            config = performanceConfig {}
        )
    }


    override fun onTerminate() {
        super.onTerminate()
        metricFlow.release()
    }
}