package com.ovais.metric_flow.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import timber.log.Timber


/**
 * Network connectivity observer.
 * Monitors WiFi, Cellular, and other network connections.
 */
interface ConnectivityObserver {
    fun install(context: Context)
    fun release()
}

class DefaultConnectivityObserver : ConnectivityObserver {
    @Volatile
    private var isInstalled = false

    private var context: Context? = null
    private var connectivityManager: ConnectivityManager? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            val networkType = getNetworkType(capabilities)
            Timber.tag("PerfMon").d("Network available: $networkType")
        }

        override fun onLost(network: Network) {
            Timber.tag("PerfMon").w("Network lost")
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val networkType = getNetworkType(networkCapabilities)

// Always safe constants
            val hasInternet =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val isMetered: Boolean =
                !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

            val downlinkKbps: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                networkCapabilities.linkDownstreamBandwidthKbps
            } else null

            Timber.tag("PerfMon").d(
                "Network changed: %s | Internet: %s | Validated: %s | Metered: %s | Downlink: %s Kbps",
                networkType,
                hasInternet,
                isValidated,
                isMetered,
                downlinkKbps ?: "N/A"
            )
        }
    }

    override fun install(context: Context) {
        requireNotNull(context) { "Context cannot be null" }

        synchronized(this) {
            if (isInstalled) {
                Timber.tag("PerfMon").w("ConnectivityObserver already installed, skipping")
                return
            }
            isInstalled = true
        }

        val appContext = context.applicationContext
        this.context = appContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (cm == null) {
            synchronized(this) {
                isInstalled = false
                this.context = null
            }
            Timber.tag("PerfMon").e("ConnectivityManager not available")
            return
        }

        connectivityManager = cm
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, networkCallback)
            Timber.tag("PerfMon").d("ConnectivityObserver installed")
        } catch (e: Exception) {
            synchronized(this) {
                isInstalled = false
                connectivityManager = null
                this.context = null
            }
            Timber.tag("PerfMon").e(e, "Failed to install ConnectivityObserver")
            throw e
        }
    }

    override fun release() {
        val cm = synchronized(this) {
            if (!isInstalled) {
                return
            }
            isInstalled = false
            connectivityManager?.also { connectivityManager = null }
        }

        cm?.let {
            try {
                it.unregisterNetworkCallback(networkCallback)
            } catch (e: IllegalArgumentException) {
                // Callback not registered - this is OK
                Timber.tag("PerfMon").d("NetworkCallback already unregistered%s", e.stackTraceToString())
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error unregistering network callback")
            }
        }

        synchronized(this) {
            context = null
        }
        Timber.tag("PerfMon").d("ConnectivityObserver released")
    }

    private fun getNetworkType(capabilities: NetworkCapabilities?): String {
        if (capabilities == null) return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
            else -> "Unknown"
        }
    }
}

