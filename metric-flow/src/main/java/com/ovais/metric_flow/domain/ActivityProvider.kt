package com.ovais.metric_flow.domain

import android.app.Activity
import android.app.Application
import android.os.Bundle
import timber.log.Timber
import java.lang.ref.WeakReference

interface ActivityProvider : Application.ActivityLifecycleCallbacks {
    val currentActivity: Activity?
    fun addOnActivityResumedListener(listener: (Activity) -> Unit)
}

class DefaultActivityProvider : ActivityProvider {

    @Volatile
    private var isInstalled = false
    
    private var currentActivityRef: WeakReference<Activity?> = WeakReference(null)
    private val listeners = mutableListOf<(Activity) -> Unit>()
    private val listenersLock = Any()
    
    fun install(application: Application) {
        synchronized(this) {
            if (isInstalled) {
                Timber.tag("PerfMon").w("ActivityProvider already installed, skipping")
                return
            }
            isInstalled = true
        }
        try {
            application.registerActivityLifecycleCallbacks(this)
        } catch (e: Exception) {
            synchronized(this) {
                isInstalled = false
            }
            throw e
        }
    }
    
    fun uninstall(application: Application) {
        synchronized(this) {
            if (!isInstalled) {
                return
            }
            isInstalled = false
        }
        try {
            application.unregisterActivityLifecycleCallbacks(this)
        } catch (e: Exception) {
            Timber.tag("PerfMon").e(e, "Error unregistering ActivityProvider")
        }
        synchronized(listenersLock) {
            listeners.clear()
        }
    }

    override val currentActivity: Activity?
        get() = currentActivityRef.get()

    override fun addOnActivityResumedListener(listener: (Activity) -> Unit) {
        requireNotNull(listener) { "Listener cannot be null" }
        synchronized(listenersLock) {
            if (!listeners.contains(listener)) {
                listeners += listener
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity == null) return
        currentActivityRef = WeakReference(activity)
        val listenersCopy: List<(Activity) -> Unit>
        synchronized(listenersLock) {
            listenersCopy = listeners.toList() // Create copy to avoid concurrent modification
        }
        listenersCopy.forEach { listener ->
            try {
                listener(activity)
            } catch (e: Exception) {
                Timber.tag("PerfMon").e(e, "Error in activity resumed listener")
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivityRef.get() == activity) {
            currentActivityRef.clear()
        }
    }

    override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
    override fun onActivityStarted(a: Activity) = Unit
    override fun onActivityStopped(a: Activity) = Unit
    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
    override fun onActivityDestroyed(a: Activity) = Unit
}