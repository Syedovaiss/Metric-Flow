package com.ovais.metric_flow.domain

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

interface ActivityProvider : Application.ActivityLifecycleCallbacks {
    val currentActivity: Activity?
    fun addOnActivityResumedListener(listener: (Activity) -> Unit)
}

class DefaultActivityProvider : ActivityProvider {

    private var currentActivityRef: WeakReference<Activity?> = WeakReference(null)
    private val listeners = mutableListOf<(Activity) -> Unit>()
    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override val currentActivity: Activity?
        get() = currentActivityRef.get()

    override fun addOnActivityResumedListener(listener: (Activity) -> Unit) {
        listeners += listener
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        listeners.forEach { it(activity) }
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