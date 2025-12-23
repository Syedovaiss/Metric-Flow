package com.ovais.metric_flow.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import timber.log.Timber

/**
 * Battery monitoring observer.
 * Tracks battery level, charging status, and health.
 */
interface BatteryObserver {
    fun install(context: Context)
    fun release()
}

class DefaultBatteryObserver : BatteryObserver {
    @Volatile
    private var isInstalled = false
    
    private var context: Context? = null
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (scale > 0) (level * 100 / scale) else -1
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f // in Celsius

            Timber.tag("PerfMon").d(
                "Battery: %d%% | Charging: %s | Health: %s | Temp: %.1f°C",
                batteryPct,
                isCharging,
                getHealthString(health),
                temperature
            )
        }
    }

    override fun install(context: Context) {
        requireNotNull(context) { "Context cannot be null" }
        
        synchronized(this) {
            if (isInstalled) {
                Timber.tag("PerfMon").w("BatteryObserver already installed, skipping")
                return
            }
            isInstalled = true
        }
        
        val appContext = context.applicationContext
        this.context = appContext
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        
        try {
            appContext.registerReceiver(batteryReceiver, filter)
            Timber.tag("PerfMon").d("BatteryObserver installed")
        } catch (e: Exception) {
            synchronized(this) {
                isInstalled = false
                this.context = null
            }
            Timber.tag("PerfMon").e(e, "Failed to install BatteryObserver")
            throw e
        }
    }

    override fun release() {
        val contextToUnregister = synchronized(this) {
            if (!isInstalled) {
                return
            }
            isInstalled = false
            context?.also { context = null }
        }
        
        contextToUnregister?.let { ctx ->
            try {
                ctx.unregisterReceiver(batteryReceiver)
            } catch (e: IllegalArgumentException) {
                // Receiver not registered - this is OK
                Timber.tag("PerfMon").d("BatteryReceiver already unregistered")
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error unregistering battery receiver")
            }
        }
        Timber.tag("PerfMon").d("BatteryObserver released")
    }

    private fun getHealthString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }
}

