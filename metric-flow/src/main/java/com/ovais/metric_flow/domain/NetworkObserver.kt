package com.ovais.metric_flow.domain


import com.ovais.metric_flow.data.NetworkClientType
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.AttributeKey
import okhttp3.Interceptor
import timber.log.Timber

fun interface NetworkObserver {
    fun install(type: NetworkClientType)
}

class DefaultNetworkObserver : NetworkObserver {

    override fun install(type: NetworkClientType) {
        when (type) {
            NetworkClientType.OkHttp -> OkHttpNetworkObserver.install()
            NetworkClientType.Ktor -> KtorNetworkObserver.install()
        }
    }
}

object OkHttpNetworkObserver {

    private var isInstalled = false

    fun install() {
        if (isInstalled) return
        isInstalled = true
        Timber.tag("PerfMon").d("OkHttp NetworkObserver installed")
    }

    fun createInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        val startNs = System.nanoTime()
        try {
            val response = chain.proceed(request)
            val tookMs = (System.nanoTime() - startNs) / 1e6
            Timber.tag("PerfMon").d(
                "OkHttp %s %s → %d (%.1f ms)",
                request.method,
                request.url,
                response.code,
                tookMs
            )
            response
        } catch (e: Exception) {
            val tookMs = (System.nanoTime() - startNs) / 1e6
            Timber.tag("PerfMon").e(
                e,
                "OkHttp %s %s failed (%.1f ms)",
                request.method,
                request.url,
                tookMs
            )
            throw e
        }
    }
}

internal object KtorNetworkObserver {

    private var isInstalled = false

    fun install() {
        if (isInstalled) return
        isInstalled = true
        Timber.tag("PerfMon").d("Ktor NetworkObserver: installing PerformancePlugin...")
    }

    private val MetricFlowPlugin = createClientPlugin("MetricFlow") {
        onRequest { request, content ->
            request.attributes.put(StartTimeKey, System.nanoTime())
        }

        onResponse { response ->
            val start = response.call.request.attributes.getOrNull(StartTimeKey)
            if (start != null) {
                val tookMs = (System.nanoTime() - start) / 1e6
                val method = response.call.request.method.value
                val url = response.call.request.url.toString()
                val code = response.status.value

                Timber.tag("PerfMon").d(
                    "Ktor %s %s → %d (%.1f ms)",
                    method,
                    url,
                    code,
                    tookMs
                )
            }
        }


    }


    private val StartTimeKey = AttributeKey<Long>("MetricFlowStartTime")
}