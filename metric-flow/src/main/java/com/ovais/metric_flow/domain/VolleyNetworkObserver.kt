package com.ovais.metric_flow.domain

import timber.log.Timber

/**
 * Network observer for Volley (Google's HTTP library).
 * 
 * Volley uses a RequestQueue and custom Request classes.
 * Since we can't easily extend Volley's Request class, we provide a listener-based approach.
 */
object VolleyNetworkObserver {

    @Volatile
    private var isInstalled = false

    fun install() {
        if (isInstalled) return
        isInstalled = true
        Timber.tag("PerfMon").d("Volley NetworkObserver installed")
    }

    fun uninstall() {
        isInstalled = false
        Timber.tag("PerfMon").d("Volley NetworkObserver uninstalled")
    }

    /**
     * Creates a monitoring listener for Volley requests.
     * 
     * Usage:
     * ```
     * val request = StringRequest(Request.Method.GET, url, 
     *     { response -> 
     *         VolleyNetworkObserver.recordSuccess(url, startTime)
     *         // handle response
     *     },
     *     { error -> 
     *         VolleyNetworkObserver.recordError(url, startTime, error)
     *         // handle error
     *     }
     * )
     * val startTime = System.nanoTime()
     * requestQueue.add(request)
     * ```
     */
    fun recordSuccess(url: String, method: String, startNs: Long, statusCode: Int) {
        val tookMs = (System.nanoTime() - startNs) / 1e6
        Timber.tag("PerfMon").d(
            "Volley %s %s → %d (%.1f ms)",
            method,
            url,
            statusCode,
            tookMs
        )
    }

    fun recordError(url: String, method: String, startNs: Long, error: Exception) {
        val tookMs = (System.nanoTime() - startNs) / 1e6
        Timber.tag("PerfMon").e(
            error,
            "Volley %s %s failed (%.1f ms)",
            method,
            url,
            tookMs
        )
    }
}

