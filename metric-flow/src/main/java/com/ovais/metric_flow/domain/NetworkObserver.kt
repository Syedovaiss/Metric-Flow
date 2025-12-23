package com.ovais.metric_flow.domain


import com.ovais.metric_flow.data.NetworkClientType
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.AttributeKey
import okhttp3.Interceptor
import timber.log.Timber

interface NetworkObserver {
    fun install(type: NetworkClientType)
    fun release()
}

class DefaultNetworkObserver : NetworkObserver {
    private var installedType: NetworkClientType? = null

    override fun install(type: NetworkClientType) {
        installedType = type
        when (type) {
            NetworkClientType.OkHttp -> OkHttpNetworkObserver.install()
            NetworkClientType.Ktor -> KtorNetworkObserver.install()
            NetworkClientType.Retrofit -> RetrofitNetworkObserver.install()
            NetworkClientType.HttpURLConnection -> HttpURLConnectionObserver.install()
            NetworkClientType.Volley -> VolleyNetworkObserver.install()
            NetworkClientType.Auto -> {
                // Auto-detect: Most apps use OkHttp/Retrofit, so default to that
                Timber.tag("PerfMon")
                    .d("Auto-detecting network type, defaulting to OkHttp/Retrofit")
                OkHttpNetworkObserver.install()
                RetrofitNetworkObserver.install()
            }
        }
    }

    override fun release() {
        when (installedType) {
            NetworkClientType.OkHttp -> OkHttpNetworkObserver.uninstall()
            NetworkClientType.Ktor -> KtorNetworkObserver.uninstall()
            NetworkClientType.Retrofit -> RetrofitNetworkObserver.uninstall()
            NetworkClientType.HttpURLConnection -> HttpURLConnectionObserver.uninstall()
            NetworkClientType.Volley -> VolleyNetworkObserver.uninstall()
            NetworkClientType.Auto -> {
                OkHttpNetworkObserver.uninstall()
                RetrofitNetworkObserver.uninstall()
            }

            null -> { /* Not installed */
            }
        }
        installedType = null
    }
}

object OkHttpNetworkObserver {

    @Volatile
    private var isInstalled = false

    fun install() {
        if (isInstalled) return
        isInstalled = true
        Timber.tag("PerfMon").d("OkHttp NetworkObserver installed")
    }

    fun uninstall() {
        isInstalled = false
        Timber.tag("PerfMon").d("OkHttp NetworkObserver uninstalled")
    }

    /**
     * Creates an OkHttp Interceptor for network monitoring.
     * Add this to your OkHttpClient using: client.addInterceptor(OkHttpNetworkObserver.createInterceptor())
     */
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

object KtorNetworkObserver {

    @Volatile
    private var isInstalled = false

    fun install() {
        if (isInstalled) return
        isInstalled = true
        Timber.tag("PerfMon").d("Ktor NetworkObserver: installing PerformancePlugin...")
    }

    fun uninstall() {
        isInstalled = false
        Timber.tag("PerfMon").d("Ktor NetworkObserver uninstalled")
    }

    /**
     * Returns the MetricFlow plugin for Ktor client.
     * Install it using: HttpClient { install(MetricFlowPlugin) }
     */
    val MetricFlowPlugin = createClientPlugin("MetricFlow") {
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