package com.ovais.metric_flow.domain

import okhttp3.Interceptor
import timber.log.Timber

/**
 * Network observer for Retrofit.
 * Retrofit uses OkHttp under the hood, so we use the same interceptor approach.
 */
object RetrofitNetworkObserver {

    @Volatile
    private var isInstalled = false

    fun install() {
        if (isInstalled) return
        isInstalled = true
        Timber.tag("PerfMon").d("Retrofit NetworkObserver installed")
    }

    fun uninstall() {
        isInstalled = false
        Timber.tag("PerfMon").d("Retrofit NetworkObserver uninstalled")
    }

    /**
     * Creates an OkHttp Interceptor for Retrofit network monitoring.
     * 
     * Retrofit uses OkHttp, so add this to your OkHttpClient:
     * 
     * ```
     * val okHttpClient = OkHttpClient.Builder()
     *     .addInterceptor(RetrofitNetworkObserver.createInterceptor())
     *     .build()
     * 
     * val retrofit = Retrofit.Builder()
     *     .baseUrl("https://api.example.com/")
     *     .client(okHttpClient)
     *     .build()
     * ```
     */
    fun createInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        val startNs = System.nanoTime()
        try {
            val response = chain.proceed(request)
            val tookMs = (System.nanoTime() - startNs) / 1e6
            Timber.tag("PerfMon").d(
                "Retrofit %s %s → %d (%.1f ms)",
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
                "Retrofit %s %s failed (%.1f ms)",
                request.method,
                request.url,
                tookMs
            )
            throw e
        }
    }
}

