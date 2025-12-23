package com.ovais.metric_flow.domain

import timber.log.Timber
import java.net.HttpURLConnection

/**
 * Network observer for HttpURLConnection (Android's built-in HTTP client).
 * 
 * This uses a wrapper approach since HttpURLConnection doesn't support interceptors.
 * Users need to wrap their connections with our helper methods.
 */
object HttpURLConnectionObserver {

    @Volatile
    private var isInstalled = false

    fun install() {
        if (isInstalled) return
        isInstalled = true
        Timber.tag("PerfMon").d("HttpURLConnection Observer installed")
    }

    fun uninstall() {
        isInstalled = false
        Timber.tag("PerfMon").d("HttpURLConnection Observer uninstalled")
    }

    /**
     * Wraps an HttpURLConnection to monitor network requests.
     * 
     * Usage:
     * ```
     * val url = URL("https://api.example.com/data")
     * val connection = url.openConnection() as HttpURLConnection
     * HttpURLConnectionObserver.wrap(connection) { conn ->
     *     // Use conn as normal
     *     conn.requestMethod = "GET"
     *     conn.connect()
     *     // Read response...
     * }
     * ```
     */
    inline fun <T> wrap(
        connection: HttpURLConnection,
        block: (HttpURLConnection) -> T
    ): T {
        val url = connection.url.toString()
        val method = connection.requestMethod.takeIf { it.isNotEmpty() } ?: "GET"
        val startNs = System.nanoTime()
        
        return try {
            val result = block(connection)
            val tookMs = (System.nanoTime() - startNs) / 1e6
            val responseCode = try {
                connection.responseCode
            } catch (e: Exception) {
                -1
            }
            
            Timber.tag("PerfMon").d(
                "HttpURLConnection %s %s → %d (%.1f ms)",
                method,
                url,
                responseCode,
                tookMs
            )
            result
        } catch (e: Exception) {
            val tookMs = (System.nanoTime() - startNs) / 1e6
            Timber.tag("PerfMon").e(
                e,
                "HttpURLConnection %s %s failed (%.1f ms)",
                method,
                url,
                tookMs
            )
            throw e
        }
    }

    /**
     * Creates a monitor for HttpURLConnection.
     * 
     * Usage:
     * ```
     * val connection = URL("https://api.example.com/data").openConnection() as HttpURLConnection
     * val monitor = HttpURLConnectionObserver.createMonitor(connection)
     * monitor.setMethod("GET")
     * connection.connect()
     * monitor.recordResponse(connection.responseCode)
     * ```
     */
    fun createMonitor(connection: HttpURLConnection): HttpURLConnectionMonitor {
        return HttpURLConnectionMonitor(connection)
    }
}

/**
 * Helper class to monitor HttpURLConnection usage.
 * Since HttpURLConnection can't be easily extended, we provide helper methods instead.
 */
class HttpURLConnectionMonitor(private val connection: HttpURLConnection) {
    private val startNs = System.nanoTime()
    private val url = connection.url.toString()
    private var method = "GET"
    private var responseCode: Int? = null

    fun setMethod(method: String) {
        this.method = method
        connection.requestMethod = method
    }

    fun getMethod(): String = method

    fun recordResponse(code: Int) {
        responseCode = code
        val tookMs = (System.nanoTime() - startNs) / 1e6
        Timber.tag("PerfMon").d(
            "HttpURLConnection %s %s → %d (%.1f ms)",
            method,
            url,
            code,
            tookMs
        )
    }

    fun recordError(error: Exception) {
        val tookMs = (System.nanoTime() - startNs) / 1e6
        Timber.tag("PerfMon").e(
            error,
            "HttpURLConnection %s %s failed (%.1f ms)",
            method,
            url,
            tookMs
        )
    }
}

