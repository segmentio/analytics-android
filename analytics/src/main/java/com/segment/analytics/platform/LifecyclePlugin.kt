package com.segment.analytics.platform

import android.app.Activity
import android.os.Bundle

// Basic interface for an plugin to consume lifecycle callbacks
interface AndroidLifecycle {
    fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
    fun onActivityStarted(activity: Activity?) {}
    fun onActivityResumed(activity: Activity?) {}
    fun onActivityPaused(activity: Activity?) {}
    fun onActivityStopped(activity: Activity?) {}
    fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
    fun onActivityDestroyed(activity: Activity?) {}
}